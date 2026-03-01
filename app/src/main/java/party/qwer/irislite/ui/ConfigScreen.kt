package party.qwer.irislite.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import party.qwer.irislite.AppConfig
import party.qwer.irislite.AppState
import party.qwer.irislite.service.IrisForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var isEnabled by remember { mutableStateOf(AppConfig.isServiceEnabled) }
    var endpoint by remember { mutableStateOf(AppConfig.webEndpoint) }
    var sendRate by remember { mutableStateOf(AppConfig.sendRate.toString()) }
    var port by remember { mutableStateOf(AppConfig.serverPort.toString()) }

    var testRoom by remember { mutableStateOf("") }
    var testRoomExpanded by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf("") }
    var configChanged by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEnabled = AppConfig.isServiceEnabled

                val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
                hasNotificationPermission = enabledPackages.contains(context.packageName)
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestPermission = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    val requestOverlayPermission = {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }

    val darkSurface = Color(0xFF1E1E1E)
    val textPrimary = Color(0xFFE3E3E3)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary, unfocusedTextColor = textPrimary,
        focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color(0xFF444444)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = darkSurface, contentColor = textPrimary)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("서비스 상태", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (isEnabled) "Running" else "Stopped",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFAAAAAA)
                            )
                        }

                        if (configChanged) {
                            Text("재시작 필요", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252), modifier = Modifier.padding(end = 12.dp))
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                isEnabled = checked
                                AppConfig.isServiceEnabled = checked
                                val serviceIntent = Intent(context, IrisForegroundService::class.java)
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                    Toast.makeText(context, "Services Started. Check Permissions.", Toast.LENGTH_LONG).show()
                                    configChanged = false
                                } else {
                                    context.stopService(serviceIntent)
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFF444444))

                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it; AppConfig.webEndpoint = it; configChanged = true },
                        label = { Text("Webserver Endpoint") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true, colors = textFieldColors
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = sendRate,
                            onValueChange = { sendRate = it; AppConfig.sendRate = it.toLongOrNull() ?: 500L; configChanged = true },
                            label = { Text("SendRate(ms)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true, colors = textFieldColors
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it; AppConfig.serverPort = it.toIntOrNull() ?: 3000; configChanged = true },
                            label = { Text("Server Port") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true, colors = textFieldColors
                        )
                    }

                    if (!hasNotificationPermission) {
                        Button(
                            onClick = requestPermission, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("알림수신 권한설정")
                        }
                    }

                    if (!hasOverlayPermission) {
                        Button(
                            onClick = requestOverlayPermission, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("앱 위에 그리기 권한설정")
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = darkSurface, contentColor = textPrimary)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Reply 테스트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    ExposedDropdownMenuBox(
                        expanded = testRoomExpanded,
                        onExpandedChange = { testRoomExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = testRoom,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("대상 Room Id 또는 이름") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = testRoomExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = testRoomExpanded,
                            onDismissRequest = { testRoomExpanded = false }
                        ) {
                            if (AppState.storedRooms.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("저장된 방 없음") },
                                    onClick = { testRoomExpanded = false }
                                )
                            } else {
                                AppState.storedRooms.forEach { room ->
                                    DropdownMenuItem(
                                        text = { Text(room) },
                                        onClick = {
                                            testRoom = room
                                            testRoomExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = testMessage,
                        onValueChange = { testMessage = it },
                        label = { Text("메시지") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            CoroutineScope(Dispatchers.IO).launch {
                                val client = OkHttpClient()

                                val jsonString = """{"type":"text","room":"$testRoom","data":"$testMessage"}"""

                                val body = jsonString.toRequestBody("application/json".toMediaType())
                                val req = Request.Builder().url("http://127.0.0.1:${AppConfig.serverPort}/reply").post(body).build()

                                try {
                                    client.newCall(req).execute().close()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) { Text("Send") }
                }
            }
        }
    }
}