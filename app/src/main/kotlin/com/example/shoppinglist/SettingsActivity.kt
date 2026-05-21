package com.example.shoppinglist

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.shoppinglist.shared.HAClient
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    private lateinit var haClient: HAClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        haClient = HAClient(this)

        // BuildConfig から自動設定（初回のみ）
        if (BuildConfig.HA_TOKEN.isNotEmpty() && haClient.getHAToken().isNullOrEmpty()) {
            haClient.saveSettings(BuildConfig.HA_URL, BuildConfig.HA_TOKEN)
        }

        setContent {
            MaterialTheme {
                SettingsScreen(haClient = haClient, onFinish = { finish() })
            }
        }
    }
}

@Composable
fun SettingsScreen(haClient: HAClient, onFinish: () -> Unit) {
    var haUrl by remember { mutableStateOf(haClient.getHAUrl()) }
    var token by remember { mutableStateOf(haClient.getHAToken() ?: "") }
    var entityId by remember { mutableStateOf(haClient.getEntityId()) }
    var isLoading by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Home Assistant 設定",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // HA URL Input
        TextField(
            value = haUrl,
            onValueChange = { haUrl = it },
            label = { Text("HA URL") },
            placeholder = { Text("http://192.168.1.12:8123") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = "ローカル: http://192.168.1.12:8123\nTailscale: http://100.x.x.x:8123 (or MagicDNS)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        // Token Input
        TextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Long-Lived Access Token") },
            placeholder = { Text("eyJ...") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = false
        )

        Text(
            text = "HA UI → プロフィール → 長期アクセストークン から取得",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        // Entity ID Input
        TextField(
            value = entityId,
            onValueChange = { entityId = it },
            label = { Text("Entity ID") },
            placeholder = { Text("todo.home") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = "デフォルト: todo.home（Bring! 統合）。Google Tasks の場合は todo.google_tasks など",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        // Connection Status
        if (connectionStatus.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (connectionStatus.startsWith("✓"))
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = connectionStatus,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        val result = haClient.testConnection()
                        connectionStatus = if (result.isSuccess) {
                            "✓ HA に接続できました"
                        } else {
                            "✗ エラー: ${result.exceptionOrNull()?.message}"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = !isLoading && token.isNotEmpty() && haUrl.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("接続テスト")
                }
            }

            Button(
                onClick = {
                    haClient.saveSettings(haUrl, token)
                    haClient.saveEntityId(entityId.ifEmpty { "todo.home" })
                    Toast.makeText(
                        context,
                        "設定を保存しました",
                        Toast.LENGTH_SHORT
                    ).show()
                    onFinish()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = token.isNotEmpty() && haUrl.isNotEmpty()
            ) {
                Text("保存")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val provider = ComponentName(context, ShoppingListWidgetReceiver::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    appWidgetManager.requestPinAppWidget(provider, null, null)
                } else {
                    Toast.makeText(context, "ウィジェット追加がサポートされていません", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("📱 ホーム画面にウィジェットを追加")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "💡 初回は「接続テスト」で HA との接続を確認してから「保存」してください。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
