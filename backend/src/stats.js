'use strict';
// Collects REAL per-user traffic from each 3X-UI panel and enforces limits.
const { getDb } = require('./db');
const { config } = require('./config');
const { XuiClient } = require('./xui');
const { decryptSecret } = require('./security');
const { getUsage, subscriptionStatus, audit } = require('./services');

let running = false;

async function syncServer(server, users, log) {
  const db = getDb();
  const client = new XuiClient(server.xui_url, server.xui_username, decryptSecret(server.xui_password_enc));
  await client.login();
  const found = [];
  for (const u of users) {
    const t = await client.getClientTraffic(u.xui_email);
    if (!t) continue;
    const now = Date.now();
    const prev = db.prepare('SELECT * FROM user_server_usage WHERE user_id=? AND server_id=?').get(u.id, server.id);
    if (!prev) {
      db.prepare('INSERT INTO user_server_usage(user_id,server_id,up_bytes,down_bytes,updated_at) VALUES (?,?,?,?,?)').run(u.id, server.id, t.up, t.down, now);
    } else {
      // If counters were reset on 3X-UI side, drop the base so we never go negative.
      const baseUp = t.up < prev.base_up ? 0 : prev.base_up;
      const baseDown = t.down < prev.base_down ? 0 : prev.base_down;
      db.prepare('UPDATE user_server_usage SET up_bytes=?,down_bytes=?,base_up=?,base_down=?,updated_at=? WHERE user_id=? AND server_id=?')
        .run(t.up, t.down, baseUp, baseDown, now, u.id, server.id);
    }
    found.push({ user: u, traffic: t });
  }
  return { client, found };
}

async function enforce(client, server, found, log) {
  const db = getDb();
  for (const { user, traffic } of found) {
    const fresh = db.prepare('SELECT * FROM users WHERE id=?').get(user.id);
    const status = subscriptionStatus(fresh, getUsage(fresh.id));
    const shouldEnable = status === 'ACTIVE';
    const row = db.prepare('SELECT disabled_on_xui FROM user_server_usage WHERE user_id=? AND server_id=?').get(user.id, server.id);
    try {
      if (!shouldEnable && traffic.enable) {
        await client.setClientEnabled(traffic.inboundId, user.xui_email, false);
        db.prepare('UPDATE user_server_usage SET disabled_on_xui=1 WHERE user_id=? AND server_id=?').run(user.id, server.id);
        audit('system', 'xui.disable_client', `user:${user.id}`, { server: server.id, reason: status });
      } else if (shouldEnable && !traffic.enable && row && row.disabled_on_xui) {
        // Re-enable only clients WE disabled (never override a manual disable in 3X-UI).
        await client.setClientEnabled(traffic.inboundId, user.xui_email, true);
        db.prepare('UPDATE user_server_usage SET disabled_on_xui=0 WHERE user_id=? AND server_id=?').run(user.id, server.id);
        audit('system', 'xui.enable_client', `user:${user.id}`, { server: server.id });
      }
    } catch (e) { log(`[stats] enforce failed for ${user.username}@${server.name}: ${e.message}`); }
  }
}

async function syncAll(log = console.log) {
  if (running) return { skipped: true };
  running = true;
  const db = getDb();
  const result = { servers: 0, errors: [] };
  try {
    const servers = db.prepare("SELECT * FROM servers WHERE enabled=1 AND xui_url<>''").all();
    const users = db.prepare("SELECT * FROM users WHERE xui_email IS NOT NULL AND xui_email<>''").all();
    for (const s of servers) {
      try {
        const { client, found } = await syncServer(s, users, log);
        if (config.enforceOnXui) await enforce(client, s, found, log);
        db.prepare('UPDATE servers SET last_sync_at=?, last_sync_error=NULL WHERE id=?').run(Date.now(), s.id);
        result.servers++;
      } catch (e) {
        db.prepare('UPDATE servers SET last_sync_error=? WHERE id=?').run(e.message, s.id);
        result.errors.push({ server: s.name, error: e.message });
        log(`[stats] ${s.name}: ${e.message}`);
      }
    }
  } finally { running = false; }
  return result;
}

function startCollector(log = console.log) {
  if (!config.statsIntervalS) return null;
  const t = setInterval(() => syncAll(log).catch((e) => log(`[stats] ${e.message}`)), config.statsIntervalS * 1000);
  t.unref();
  setTimeout(() => syncAll(log).catch(() => {}), 3000).unref();
  return t;
}

module.exports = { syncAll, startCollector };
