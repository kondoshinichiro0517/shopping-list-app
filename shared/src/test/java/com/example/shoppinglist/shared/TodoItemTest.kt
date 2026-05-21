package com.example.shoppinglist.shared

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TodoItemTest {
    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    @Test
    fun testTodoItemSerialization() {
        val item = TodoItem(uid = "uid-123", name = "牛乳", complete = false)
        val adapter = moshi.adapter(TodoItem::class.java)

        val json = adapter.toJson(item)
        assertNotNull(json)
        assert(json.contains("牛乳"))
    }

    @Test
    fun testTodoItemDeserialization() {
        val json = """{"uid":"uid-123","name":"パン","complete":false}"""
        val adapter = moshi.adapter(TodoItem::class.java)

        val item = adapter.fromJson(json)
        assertNotNull(item)
        assertEquals("パン", item?.name)
        assertEquals("uid-123", item?.uid)
        assertEquals(false, item?.complete)
    }

    @Test
    fun testTodoItemRoundTrip() {
        val original = TodoItem(uid = "uid-456", name = "卵", complete = false)
        val adapter = moshi.adapter(TodoItem::class.java)

        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json)

        assertEquals(original.uid, deserialized?.uid)
        assertEquals(original.name, deserialized?.name)
        assertEquals(original.complete, deserialized?.complete)
    }

    @Test
    fun testTodoItemWithCompleteTrue() {
        val item = TodoItem(uid = "uid-789", name = "チーズ", complete = true)
        val adapter = moshi.adapter(TodoItem::class.java)

        val json = adapter.toJson(item)
        val deserialized = adapter.fromJson(json)

        assertEquals(true, deserialized?.complete)
    }

    @Test
    fun testTodoItemNullUid() {
        val item = TodoItem(uid = null, name = "トマト", complete = false)
        val adapter = moshi.adapter(TodoItem::class.java)

        val json = adapter.toJson(item)
        val deserialized = adapter.fromJson(json)

        assertEquals(null, deserialized?.uid)
        assertEquals("トマト", deserialized?.name)
    }
}
