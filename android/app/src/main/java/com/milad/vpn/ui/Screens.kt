package com.milad.vpn.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.milad.vpn.BuildConfig
import com.milad.vpn.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milad.vpn.data.Account
import com.milad.vpn.vpn.Phase
import com.milad.vpn.vpn.VpnStatus
import kotlinx.coroutines.delay

private val Primary = Color(0xFF3B5BFD)
private val Ok = Color(0xFF16A34A)
private val Warn = Color(0xFFD97706)
private val Bad = Color(0xFFDC2626)
private val LightBg = Color(0xFFF4F6FB)
private val DarkBg = Color(0xFF0F1117)

private const val DEVELOPER_NAME = "Milad Rahmani"

private var lastVibrationAt = 0L

/**
 * Short, subtle haptic tick. [strong] is used only for connect/disconnect.
 * Rate-limited so rapid taps never produce a continuous buzz.
 */
private fun vibrate(context: Context, strong: Boolean = false) {
    val now = android.os.SystemClock.elapsedRealtime()
    if (now - lastVibrationAt < 80) return
    lastVibrationAt = now
    val durationMs = if (strong) 35L else 15L
    try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        val amplitude = if (vibrator.hasAmplitudeControl()) {
            if (strong) 120 else 60
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
    } catch (_: Exception) {
    }
}

@Composable
private fun HapticButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    Button(
        onClick = {
            vibrate(context)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
private fun HapticOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            vibrate(context)
            onClick()
        },
        modifier = modifier,
        content = content
    )
}

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF7C8CFF),
            background = DarkBg,
            surface = Color(0xFF181B23),
            surfaceVariant = Color(0xFF232733)
        )
    } else {
        lightColorScheme(
            primary = Primary,
            background = LightBg,
            surface = Color.White
        )
    }

    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun AppRoot(
    vm: MainViewModel,
    onStartVpn: () -> Unit,
    english: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    AppTheme(darkTheme = darkMode) {
        if (!ui.loggedIn) {
            LoginScreen(
                loading = ui.loading,
                error = ui.loginError,
                english = english,
                onLogin = vm::login
            )
        } else {
            MainScaffold(
                vm = vm,
                ui = ui,
                onStartVpn = onStartVpn,
                english = english,
                darkMode = darkMode,
                onLanguageChange = onLanguageChange,
                onDarkModeChange = onDarkModeChange
            )
        }
    }
}

// ---------------------------------------------------------------------------------- login

@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    english: Boolean,
    onLogin: (String, String) -> Unit
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(surface),
            modifier = Modifier.widthIn(max = 380.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Filled.Shield,
                    null,
                    tint = Primary,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    "Xnet",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (english) "Sign in to your account" else "ورود به حساب",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    user,
                    { user = it },
                    label = { Text(if (english) "Username" else "نام کاربری") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    pass,
                    { pass = it },
                    label = { Text(if (english) "Password" else "رمز عبور") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (show) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton({
                            vibrate(context)
                            show = !show
                        }) {
                            Icon(
                                if (show) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                null
                            )
                        }
                    }
                )

                if (error != null) {
                    Text(
                        error,
                        color = Bad,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                HapticButton(
                    enabled = !loading,
                    onClick = { onLogin(user, pass) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    content = {
                        if (loading) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (english) "Sign in" else "ورود")
                        }
                    }
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------- shell

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    vm: MainViewModel,
    ui: UiState,
    onStartVpn: () -> Unit,
    english: Boolean,
    darkMode: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val vpn by vm.vpn.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(ui.info) {
        ui.info?.let { snack.showSnackbar(it) }
    }

    LaunchedEffect(vpn.error) {
        vpn.error?.let { snack.showSnackbar(it) }
    }

    LaunchedEffect(vpn.stopCode) {
        if (vpn.stopCode != null) vm.refreshNow()
    }

    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val labels = if (english) {
                    listOf("Home", "Servers", "Account")
                } else {
                    listOf("خانه", "سرورها", "حساب")
                }

                listOf(
                    Triple(labels[0], Icons.Filled.Home, 0),
                    Triple(labels[1], Icons.Filled.Public, 1),
                    Triple(labels[2], Icons.Filled.Person, 2)
                ).forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = {
                            vibrate(context)
                            tab = index
                        },
                        icon = { Icon(icon, null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            when (tab) {
                0 -> HomeScreen(
                    ui,
                    vpn,
                    english
                ) { vm.toggle(onStartVpn) }

                1 -> ServersScreen(
                    ui,
                    english,
                    vm::selectConfig,
                    connectedId = if (vpn.phase == Phase.CONNECTED) vpn.configId else null
                )

                else -> AccountScreen(
                    acc = ui.account,
                    english = english,
                    darkMode = darkMode,
                    onLanguageChange = onLanguageChange,
                    onDarkModeChange = onDarkModeChange,
                    onRefresh = vm::refreshNow,
                    onLogout = vm::logout
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------- home

@Composable
fun HomeScreen(
    ui: UiState,
    vpn: VpnStatus,
    english: Boolean,
    onToggle: () -> Unit
) {
    val acc = ui.account

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (ui.blockMessage != null) {
            Banner(ui.blockMessage, Bad)
        }

        Spacer(Modifier.height(8.dp))

        ConnectButton(vpn.phase, onToggle)

        val (label, color) = when (vpn.phase) {
            Phase.CONNECTED ->
                (if (english) "Connected" else "متصل") to Ok

            Phase.CONNECTING ->
                (if (english) "Connecting…" else "در حال اتصال…") to Warn

            Phase.RECONNECTING ->
                (if (english) "Reconnecting…" else "در حال اتصال مجدد…") to Warn

            Phase.DISCONNECTED ->
                (if (english) "Disconnected" else "قطع") to Color.Gray
        }

        Text(
            label,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            vpn.configName?.let {
                if (english) "Server: $it" else "سرور: $it"
            } ?: autoLabel(ui, english),
            color = Color.Gray,
            fontSize = 14.sp
        )

        if (vpn.phase == Phase.CONNECTED) {
            var tick by remember { mutableIntStateOf(0) }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    tick++
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    if (english) "Download" else "دانلود",
                    Format.speed(vpn.downSpeed),
                    Icons.Filled.ArrowDownward,
                    Modifier.weight(1f)
                )

                StatCard(
                    if (english) "Upload" else "آپلود",
                    Format.speed(vpn.upSpeed),
                    Icons.Filled.ArrowUpward,
                    Modifier.weight(1f)
                )
            }

            key(tick) {
                Text(
                    if (english) {
                        "Connection: ${Format.duration(vpn.connectedSince)} · Ping: ${vpn.pingMs?.let { Format.num(it) } ?: "—"} ms"
                    } else {
                        "مدت اتصال: ${Format.duration(vpn.connectedSince)} · پینگ: ${vpn.pingMs?.let { Format.num(it) } ?: "—"} ms"
                    },
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        if (acc != null) UsageCard(acc, english)
    }
}

private fun autoLabel(ui: UiState, english: Boolean): String =
    if (ui.selectedConfigId < 0) {
        if (english) "Auto server selection" else "انتخاب خودکار سرور"
    } else {
        ui.configs.firstOrNull { it.id == ui.selectedConfigId }?.name
            ?: if (english) "Auto server selection" else "انتخاب خودکار سرور"
    }

@Composable
fun ConnectButton(
    phase: Phase,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        when (phase) {
            Phase.CONNECTED -> Ok
            Phase.DISCONNECTED -> Primary
            else -> Warn
        },
        label = "btn"
    )
    val context = LocalContext.current

    Box(
        Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(bg.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(bg)
                .clickable {
                    vibrate(context, strong = true)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (phase == Phase.CONNECTING || phase == Phase.RECONNECTING) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = if (phase == Phase.CONNECTED) "Disconnect" else "Connect",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UsageCard(
    acc: Account,
    english: Boolean = false
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row {
                Text(
                    if (english) "Usage" else "حجم مصرفی",
                    Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${Format.bytes(acc.usedBytes)} ${if (english) "of" else "از"} ${
                        if (acc.unlimited) {
                            if (english) "Unlimited" else "نامحدود"
                        } else {
                            Format.bytes(acc.quotaBytes)
                        }
                    }"
                )
            }

            if (!acc.unlimited) {
                val p =
                    if (acc.quotaBytes > 0)
                        (acc.usedBytes.toFloat() / acc.quotaBytes).coerceIn(0f, 1f)
                    else 0f

                LinearProgressIndicator(
                    progress = { p },
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = if (p >= 0.9f) Bad else Primary
                )

                Row {
                    Text(
                        if (english) "Remaining" else "باقی‌مانده",
                        Modifier.weight(1f),
                        color = Color.Gray
                    )
                    Text(Format.bytes(acc.remainingBytes))
                }
            }

            HorizontalDivider()

            Row {
                Text(
                    if (english) "Expiration" else "تاریخ انقضا",
                    Modifier.weight(1f),
                    color = Color.Gray
                )
                Text(Format.date(acc.expiresAt))
            }

            acc.daysLeft?.let {
                Row {
                    Text(
                        if (english) "Days remaining" else "اعتبار باقی‌مانده",
                        Modifier.weight(1f),
                        color = Color.Gray
                    )
                    Text(
                        if (english) "${Format.num(it)} days" else "${Format.num(it)} روز",
                        color = if (it <= 3) Bad else Color.Unspecified
                    )
                }
            }

            acc.usageSyncedAt?.let {
                Text(
                    if (english) {
                        "Last statistics update: ${Format.date(it)}"
                    } else {
                        "آخرین به‌روزرسانی آمار: ${Format.date(it)}"
                    },
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun Banner(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, null, tint = color)
            Spacer(Modifier.width(8.dp))
            Text(text, color = color)
        }
    }
}

// ------------------------------------------------------------------------------- servers

@Composable
fun ServersScreen(
    ui: UiState,
    english: Boolean,
    onSelect: (Long) -> Unit,
    connectedId: Long? = null
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                if (english) "Select server" else "انتخاب سرور",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ServerRow(
                if (english) "Auto (best server + failover)" else "خودکار (بهترین سرور + جابه‌جایی در صورت قطعی)",
                "",
                ui.selectedConfigId < 0
            ) {
                onSelect(-1)
            }
        }

        items(ui.configs, key = { it.id }) { c ->
            ServerRow(
                c.name,
                "${c.country} · ${c.protocol.uppercase()}",
                ui.selectedConfigId == c.id,
                connectedLabel = if (connectedId == c.id) {
                    if (english) "Connected" else "متصل"
                } else null
            ) {
                onSelect(c.id)
            }
        }

        if (ui.configs.isEmpty()) {
            item {
                Text(
                    ui.blockMessage ?: if (english) "No server available" else "سروری در دسترس نیست",
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ServerRow(
    title: String,
    sub: String,
    selected: Boolean,
    connectedLabel: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        Modifier
            .fillMaxWidth()
            .clickable {
                vibrate(context)
                onClick()
            },
        colors = CardDefaults.cardColors(
            if (selected)
                Primary.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected,
                {
                    vibrate(context)
                    onClick()
                }
            )

            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)

                if (sub.isNotBlank()) {
                    Text(
                        sub,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            if (connectedLabel != null) {
                Text(
                    connectedLabel,
                    color = Ok,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------- account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    acc: Account?,
    english: Boolean,
    darkMode: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    var confirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                if (english) "Account" else "حساب کاربری",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (acc != null) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row {
                            Text(
                                if (english) "Username" else "نام کاربری",
                                Modifier.weight(1f),
                                color = Color.Gray
                            )
                            Text(acc.username)
                        }

                        Row {
                            Text(
                                if (english) "Status" else "وضعیت",
                                Modifier.weight(1f),
                                color = Color.Gray
                            )

                            Text(
                                when (acc.status) {
                                    "ACTIVE" -> if (english) "Active" else "فعال"
                                    "EXPIRED" -> if (english) "Expired" else "منقضی"
                                    "QUOTA_EXCEEDED" -> if (english) "Quota exceeded" else "اتمام حجم"
                                    "BLOCKED" -> if (english) "Blocked" else "مسدود"
                                    else -> acc.status
                                },
                                color = if (acc.isActive) Ok else Bad
                            )
                        }

                        Row {
                            Text(
                                if (english) "Download" else "دانلود",
                                Modifier.weight(1f),
                                color = Color.Gray
                            )
                            Text(Format.bytes(acc.downloadBytes))
                        }

                        Row {
                            Text(
                                if (english) "Upload" else "آپلود",
                                Modifier.weight(1f),
                                color = Color.Gray
                            )
                            Text(Format.bytes(acc.uploadBytes))
                        }
                    }
                }
            }

            item {
                UsageCard(acc, english)
            }
        }

        // Developer
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Icon(Icons.Filled.Code, null, tint = Primary)
                        Spacer(Modifier.width(10.dp))

                        Column {
                            Text(
                                if (english) "Developer" else "توسعه‌دهنده",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            Text(
                                DEVELOPER_NAME,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "${stringResource(R.string.app_name)} · ${if (english) "Version" else "نسخه"} ${BuildConfig.VERSION_NAME}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Appearance
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (english) "Appearance" else "ظاهر",
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (darkMode) Icons.Filled.DarkMode
                            else Icons.Filled.LightMode,
                            null,
                            tint = Primary
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            if (english) "Dark mode" else "حالت تاریک",
                            Modifier.weight(1f)
                        )

                        Switch(
                            checked = darkMode,
                            onCheckedChange = {
                                vibrate(context)
                                onDarkModeChange(it)
                            }
                        )
                    }
                }
            }
        }

        // Language
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (english) "Language" else "زبان",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !english,
                            onClick = {
                                vibrate(context)
                                onLanguageChange(false)
                            },
                            label = { Text("فارسی") },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = english,
                            onClick = {
                                vibrate(context)
                                onLanguageChange(true)
                            },
                            label = { Text("English") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            HapticOutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Icon(Icons.Filled.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (english) "Refresh information"
                        else "به‌روزرسانی اطلاعات"
                    )
                }
            )
        }

        item {
            HapticButton(
                onClick = { confirm = true },
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Icon(Icons.Filled.Logout, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (english) "Sign out" else "خروج از حساب")
                }
            )
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = {
                Text(if (english) "Sign out" else "خروج")
            },
            text = {
                Text(
                    if (english) {
                        "The VPN connection will be stopped and login information will be removed from this device. Continue?"
                    } else {
                        "اتصال قطع و اطلاعات ورود از این دستگاه پاک می‌شود. ادامه می‌دهید؟"
                    }
                )
            },
            confirmButton = {
                TextButton({
                    confirm = false
                    vibrate(context)
                    onLogout()
                }) {
                    Text(
                        if (english) "Sign out" else "خروج",
                        color = Bad
                    )
                }
            },
            dismissButton = {
                TextButton({
                    confirm = false
                    vibrate(context)
                }) {
                    Text(if (english) "Cancel" else "انصراف")
                }
            }
        )
    }
}
