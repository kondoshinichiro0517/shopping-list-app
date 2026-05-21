package com.example.shoppinglist.shared

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Home Assistant API クライアント
 * HA の REST API を呼び出してリスト取得・アイテム削除を行う
 */
class HAClient(context: Context) {
    private val context = context.applicationContext
    private val masterKey = MasterKey.Builder(this.context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        this.context,
        "ha_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private val moshi: Moshi by lazy {
            Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        }

        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        }
    }

    // 設定の保存・読み込み
    fun saveSettings(haUrl: String, token: String) {
        encryptedPrefs.edit().apply {
            putString("ha_url", haUrl)
            putString("ha_token", token)
            apply()
        }
    }

    fun getHAUrl(): String = encryptedPrefs.getString("ha_url", "http://192.168.1.12:8123") ?: "http://192.168.1.12:8123"
    fun getHAToken(): String? = encryptedPrefs.getString("ha_token", null)
    fun getEntityId(): String = encryptedPrefs.getString("entity_id", "todo.home") ?: "todo.home"

    fun saveEntityId(entityId: String) {
        encryptedPrefs.edit().apply {
            putString("entity_id", entityId)
            apply()
        }
    }

    /**
     * HA に接続できるか確認（設定テスト用）
     */
    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getHAToken()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(
                    Exception("認証エラー: Token が設定されていません。Settings で HA Token を入力してください。")
                )
            }

            val url = getHAUrl().trimEnd('/') + "/api/"
            if (url.isEmpty()) {
                return@withContext Result.failure(
                    Exception("設定エラー: HA URL が空です。Settings で HA URL を入力してください。")
                )
            }

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .build()

            val response = HAClient.httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = when (response.code) {
                    401 -> "認証エラー (401): Token が無効です。Settings で HA Token を確認してください。"
                    403 -> "アクセス拒否 (403): Token に権限がありません。"
                    404 -> "接続エラー (404): HA URL が間違っています。http://192.168.1.12:8123 を確認。"
                    500 -> "サーバーエラー (500): HA サーバーに問題があります。HA を再起動してください。"
                    503 -> "サービス利用不可 (503): HA サーバーが起動中です。しばらく待ってから再試行。"
                    else -> "HTTP エラー (${response.code}): HA サーバーからエラーレスポンス。"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * todo.home のリストを取得
     */
    suspend fun getItems(): List<TodoItem> = withContext(Dispatchers.IO) {
        try {
            val token = getHAToken()
            if (token.isNullOrEmpty()) {
                android.util.Log.w("HAClient", "getItems: Token が未設定")
                return@withContext emptyList()
            }

            val entityId = getEntityId()
            val url = getHAUrl().trimEnd('/') + "/api/states/$entityId"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .build()

            val response = HAClient.httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext emptyList()
                val adapter = HAClient.moshi.adapter(HAStateResponse::class.java)
                val state = adapter.fromJson(body)
                state?.attributes?.items ?: emptyList()
            } else {
                android.util.Log.e("HAClient", "getItems: HTTP ${response.code} - $entityId")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("HAClient", "getItems: 例外発生", e)
            emptyList()
        }
    }

    /**
     * アイテムを削除（完了としてマーク）
     */
    suspend fun removeItem(itemName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getHAToken()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(
                    Exception("認証エラー: Token が設定されていません。")
                )
            }

            if (itemName.isBlank()) {
                return@withContext Result.failure(
                    Exception("入力エラー: アイテム名が空です。")
                )
            }

            val url = getHAUrl().trimEnd('/') + "/api/services/todo/remove_item"
            val entityId = getEntityId()

            val request = HAServiceRequest(
                entity_id = entityId,
                item = itemName
            )
            val adapter = HAClient.moshi.adapter(HAServiceRequest::class.java)
            val json = adapter.toJson(request)

            val requestBody = json.toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            val response = HAClient.httpClient.newCall(httpRequest).execute()
            return@withContext if (response.isSuccessful) {
                android.util.Log.d("HAClient", "removeItem: 削除成功 - $itemName")
                Result.success(Unit)
            } else {
                val errorMsg = when (response.code) {
                    401 -> "認証エラー (401): Token が無効です。"
                    404 -> "エラー (404): エンティティ ID '$entityId' が見つかりません。"
                    else -> "削除エラー (${response.code}): ${response.body?.string()}"
                }
                android.util.Log.e("HAClient", "removeItem: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("HAClient", "removeItem: 例外発生", e)
            Result.failure(e)
        }
    }

}
