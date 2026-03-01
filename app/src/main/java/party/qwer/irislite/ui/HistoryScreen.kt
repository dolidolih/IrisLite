package party.qwer.irislite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import party.qwer.irislite.AppState

@Composable
fun HistoryScreen() {
    val darkSurface = Color(0xFF1E1E1E)
    val textPrimary = Color(0xFFE3E3E3)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        // --- Section 1: Stored Room List ---
        item {
            Text("Stored Room List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        if (AppState.storedRooms.isEmpty()) {
            item { Text("No stored rooms yet.", color = Color.Gray) }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = darkSurface, contentColor = textPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AppState.storedRooms.forEach { room ->
                            Text("• $room", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD4D4D4), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF444444))
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- Section 2: Notification History ---
        item {
            Text("Notification History (Latest 10)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        items(AppState.notificationHistory) { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = darkSurface, contentColor = textPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Room: ${event.room} (ID: ${event.roomId})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }

                    Text("Sender: ${event.senderName} (ID: ${event.senderId})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Group Chat: ${event.isGroupChat}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Text: ${event.text}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF000000)).padding(12.dp)) {
                        Text(
                            text = "Bundle Dump: ${event.rawDump}",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace, maxLines = 100
                        )
                    }
                }
            }
        }
    }
}