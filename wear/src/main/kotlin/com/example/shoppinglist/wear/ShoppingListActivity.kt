package com.example.shoppinglist.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.shoppinglist.shared.HAClient
import com.example.shoppinglist.shared.TodoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShoppingListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ShoppingListScreen(HAClient(this))
            }
        }
    }
}

@Composable
fun ShoppingListScreen(haClient: HAClient) {
    var items by remember { mutableStateOf<List<TodoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 初回読み込み
    LaunchedEffect(Unit) {
        loadItems(haClient) { result ->
            items = result.getOrElse { emptyList() }
            error = result.exceptionOrNull()?.message ?: ""
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Text(
                text = "読み込み中...",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface
            )
        } else if (error.isNotEmpty()) {
            Text(
                text = "エラー: $error",
                fontSize = 12.sp,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ヘッダー
                Text(
                    text = "🛒 買い物リスト",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // リスト
                if (items.isEmpty()) {
                    Text(
                        text = "リストが空です",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    ScalingLazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(items, key = { it.uid ?: it.name }) { item ->
                            ShoppingListItemChip(
                                item = item,
                                onRemove = { itemName ->
                                    scope.launch {
                                        haClient.removeItem(itemName)
                                        delay(500)
                                        loadItems(haClient) { result ->
                                            items = result.getOrElse { items.filter { it.name != itemName } }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // リフレッシュボタン
                Chip(
                    label = { Text("🔄 再読み込み", fontSize = 11.sp) },
                    onClick = {
                        isLoading = true
                        scope.launch {
                            loadItems(haClient) { result ->
                                items = result.getOrElse { emptyList() }
                                error = result.exceptionOrNull()?.message ?: ""
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}

@Composable
fun ShoppingListItemChip(item: TodoItem, onRemove: (String) -> Unit) {
    Chip(
        onClick = { onRemove(item.name) },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = item.name,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "削除",
                modifier = Modifier.size(16.dp)
            )
        },
        colors = ChipDefaults.secondaryChipColors()
    )
}

private suspend fun loadItems(
    haClient: HAClient,
    callback: (Result<List<TodoItem>>) -> Unit
) {
    try {
        val items = haClient.getItems()
        callback(Result.success(items))
    } catch (e: Exception) {
        callback(Result.failure(e))
    }
}
