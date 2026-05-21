package com.example.shoppinglist

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionParametersOf
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.state.GlanceStateDefinition
import androidx.glance.appwidget.state.PreferencesGlanceStateDefinition
import androidx.glance.layout.*
import androidx.glance.material3.GlanceTheme
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.shoppinglist.shared.HAClient
import com.example.shoppinglist.shared.TodoItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch

/**
 * Shopping List Widget - Glance で実装
 * WorkManager がバックグラウンドで定期的に更新したデータを表示
 *
 * Note: Widget functionality is temporarily disabled due to Glance dependency resolution issues.
 * TODO: Resolve Glance 1.2.0-rc01 compatibility and re-enable widget functionality.
 */
/*
class ShoppingListWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single
    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val prefs = currentState<Preferences>()
        val itemStrings = prefs[PreferencesKeys.SHOPPING_ITEMS] ?: emptySet()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(TodoItem::class.java)
        val items = itemStrings.mapNotNull { adapter.fromJson(it) }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // ヘッダー
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🛒 買い物リスト",
                    style = TextStyle(fontSize = 12.sp)
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // リスト表示
            if (items.isEmpty()) {
                Text(
                    text = "リストが空です",
                    style = TextStyle(fontSize = 12.sp),
                    modifier = GlanceModifier.padding(4.dp)
                )
            } else {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {
                    items(items) { item ->
                        ShoppingListItemRow(item, context)
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // リフレッシュボタン
            Button(
                text = "🔄 更新",
                onClick = actionRunCallback<RefreshWidgetCallback>(),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
        }
    }
}

@Composable
fun ShoppingListItemRow(item: TodoItem, context: Context) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(ColorProvider(day = android.graphics.Color.parseColor("#f5f5f5"))),
        horizontalAlignment = Alignment.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• ${item.name}",
            style = TextStyle(fontSize = 11.sp),
            modifier = GlanceModifier
                .padding(horizontal = 6.dp)
                .defaultWeight()
        )

        Button(
            text = "✓",
            onClick = actionRunCallback<RemoveItemCallback>(
                actionParametersOf(RemoveItemCallback.itemNameKey to item.name)
            ),
            modifier = GlanceModifier
                .size(24.dp)
                .padding(2.dp)
        )
    }
}

class RemoveItemCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val itemName = parameters.get(RemoveItemCallback.itemNameKey) ?: return
        val haClient = HAClient(context)

        try {
            haClient.removeItem(itemName)
            // 削除後、WorkManager に即座に再取得させる
            SyncWorker.schedulePeriodic(context)
        } catch (e: Exception) {
            // エラーは無視（widget のエラー表示は限定的なため）
        }
    }

    companion object {
        val itemNameKey = ActionParameters.Key<String>("item_name")
    }
}

class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val haClient = HAClient(context)
        try {
            val items = haClient.getItems()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(TodoItem::class.java)
            val itemStrings = items.map { adapter.toJson(it) }.toSet()

            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOPPING_ITEMS] = itemStrings
            }
        } catch (e: Exception) {
            // エラーは無視
        }
    }
}

class ShoppingListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShoppingListWidget()

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        if (intent.action == "android.appwidget.action.APPWIDGET_UPDATE") {
            // ウィジェット更新時に WorkManager をスケジュール
            SyncWorker.schedulePeriodic(context)
        }
    }
}
*/
