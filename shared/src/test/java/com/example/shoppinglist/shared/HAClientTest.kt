package com.example.shoppinglist.shared

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HAClientTest {
    private lateinit var haClient: HAClient

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        haClient = HAClient(context)
    }

    @Test
    fun testDefaultHAUrl() {
        val url = haClient.getHAUrl()
        assertEquals("http://192.168.1.12:8123", url)
    }

    @Test
    fun testDefaultEntityId() {
        val entityId = haClient.getEntityId()
        assertEquals("todo.home", entityId)
    }

    @Test
    fun testSaveAndGetSettings() {
        val testUrl = "http://192.168.1.100:8123"
        val testToken = "test-token-12345"

        haClient.saveSettings(testUrl, testToken)

        assertEquals(testUrl, haClient.getHAUrl())
        assertEquals(testToken, haClient.getHAToken())
    }

    @Test
    fun testSaveAndGetEntityId() {
        val testEntityId = "todo.google_tasks"

        haClient.saveEntityId(testEntityId)

        assertEquals(testEntityId, haClient.getEntityId())
    }

    @Test
    fun testEntityIdDefaultValue() {
        val entityId = haClient.getEntityId()
        assertTrue(entityId.isNotEmpty())
    }

    @Test
    fun testTokenNullWhenNotSet() {
        val token = haClient.getHAToken()
        // Token が未設定時は null または空文字
        assertTrue(token == null || token.isEmpty())
    }

    @Test
    fun testUrlNotNull() {
        val url = haClient.getHAUrl()
        assertNotNull(url)
        assertTrue(url.isNotEmpty())
    }

    @Test
    fun testSingletonMoshi() {
        // Companion object の lazy Moshi が同じインスタンスを返すこと
        val moshi1 = HAClient.moshi
        val moshi2 = HAClient.moshi
        assertEquals(moshi1, moshi2)
    }

    @Test
    fun testSingletonHttpClient() {
        // Companion object の lazy OkHttpClient が同じインスタンスを返すこと
        val client1 = HAClient.httpClient
        val client2 = HAClient.httpClient
        assertEquals(client1, client2)
    }
}
