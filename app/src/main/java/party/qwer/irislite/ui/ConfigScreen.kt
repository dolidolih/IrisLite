package party.qwer.irislite.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Link
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import party.qwer.irislite.AppColors
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEnabled = AppConfig.isServiceEnabled
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val seamlessTextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = AppColors.InputBg,
        unfocusedContainerColor = AppColors.InputBg,
        disabledContainerColor = AppColors.InputBg,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedTextColor = AppColors.TextMain,
        unfocusedTextColor = AppColors.TextMain,
        focusedLabelColor = AppColors.PrimaryAccent,
        unfocusedLabelColor = AppColors.TextSub,
        cursorColor = AppColors.PrimaryAccent
    )

    val elementShape = RoundedCornerShape(12.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        item {
            Text("서비스 설정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextMain)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("백그라운드 서비스", fontWeight = FontWeight.SemiBold, color = AppColors.TextMain)
                            Text(
                                text = if (isEnabled) "작동중" else "정지됨",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isEnabled) AppColors.SuccessVivid else AppColors.TextSub
                            )
                        }
                        if (configChanged) {
                            Text("재시작 필요", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.ErrorVivid, modifier = Modifier.padding(end = 12.dp))
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                val action = if (checked) "party.qwer.irislite.START" else "party.qwer.irislite.STOP"
                                val serviceIntent = Intent(context, IrisForegroundService::class.java).apply { this.action = action }
                                context.startForegroundService(serviceIntent)
                                isEnabled = checked
                                configChanged = false
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.TextMain,
                                checkedTrackColor = AppColors.PrimaryAccent,
                                uncheckedThumbColor = AppColors.TextSub,
                                uncheckedTrackColor = AppColors.InputBg,
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    TextField(
                        value = endpoint,
                        onValueChange = { endpoint = it; AppConfig.webEndpoint = it; configChanged = true },
                        label = { Text("웹서버 엔드포인트") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = AppColors.TextSub) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = elementShape,
                        singleLine = true,
                        colors = seamlessTextFieldColors
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextField(
                            value = sendRate,
                            onValueChange = { sendRate = it; AppConfig.sendRate = it.toLongOrNull() ?: 500L; configChanged = true },
                            label = { Text("발송주기(ms)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = elementShape,
                            singleLine = true,
                            colors = seamlessTextFieldColors
                        )
                        TextField(
                            value = port,
                            onValueChange = { port = it; AppConfig.serverPort = it.toIntOrNull() ?: 3000; configChanged = true },
                            label = { Text("서비스포트") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = elementShape,
                            singleLine = true,
                            colors = seamlessTextFieldColors
                        )
                    }
                }
            }
        }

        item {
            Text("답장 테스트", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextMain)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = testRoomExpanded,
                        onExpandedChange = { testRoomExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = testRoom,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("방 이름 또는 ID") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = testRoomExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = elementShape,
                            singleLine = true,
                            colors = seamlessTextFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = testRoomExpanded,
                            onDismissRequest = { testRoomExpanded = false },
                            modifier = Modifier.background(AppColors.CardBg)
                        ) {
                            if (AppState.storedRooms.isEmpty()) {
                                DropdownMenuItem(text = { Text("저장된 방 없음", color = AppColors.TextSub) }, onClick = { testRoomExpanded = false })
                            } else {
                                AppState.storedRooms.forEach { room ->
                                    DropdownMenuItem(
                                        text = { Text(room.name, color = AppColors.TextMain) },
                                        onClick = { testRoom = room.name; testRoomExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(room.id, color = AppColors.TextMain) },
                                        onClick = { testRoom = room.id; testRoomExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    TextField(
                        value = testMessage,
                        onValueChange = { testMessage = it },
                        label = { Text("메시지") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = elementShape,
                        colors = seamlessTextFieldColors
                    )

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            CoroutineScope(Dispatchers.IO).launch {
                                val client = OkHttpClient()
                                val jsonString = """{"type":"text","room":"$testRoom","data":"$testMessage"}"""
                                val body = jsonString.toRequestBody("application/json".toMediaType())
                                val req = Request.Builder().url("http://127.0.0.1:${AppConfig.serverPort}/reply").post(body).build()
                                try { client.newCall(req).execute().close() } catch (e: Exception) { e.printStackTrace() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = elementShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryAccent, contentColor = AppColors.TextMain)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("메시지 보내기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}