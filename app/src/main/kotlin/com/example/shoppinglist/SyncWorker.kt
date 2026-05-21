package com.example.shoppinglist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import com.example.shoppinglist.shared.HAClient
import com.example.shoppinglist.shared.TodoItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shopping_list")

object PreferencesKeys {
    val SHOPPING_ITEMS = stringSetPreferencesKey("shopping_items")
}

/**
 * WorkManager で 15 分ごとに HA からリストを取得し DataStore に保存
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val haClient = HAClient(context)

    override suspend fun doWork(): Result {
        return try {
            val items = haClient.getItems()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(TodoItem::class.java)

            val itemStrings = items.map { adapter.toJson(it) }.toSet()

            applicationContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOPPING_ITEMS] = itemStrings
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "shopping_list_sync"

        fun schedulePeriodic(context: Context) {
            val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
            )
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

suspend fun getShoppingItemsFromDataStore(context: Context): List<TodoItem> {
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(TodoItem::class.java)

    val preferences = context.dataStore.data.first()
    val itemStrings = preferences[PreferencesKeys.SHOPPING_ITEMS] ?: return emptyList()

    return itemStrings.mapNotNull { adapter.fromJson(it) }
}
