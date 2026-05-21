package com.example.shoppinglist.shared

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TodoItem(
    @Json(name = "summary")
    val summary: String,
    @Json(name = "uid")
    val uid: String? = null,
    val status: String = "needs_action" // "needs_action" or "completed"
) {
    val name: String get() = summary
    val isCompleted: Boolean get() = status == "completed"
}

@JsonClass(generateAdapter = true)
data class HAStateResponse(
    val entity_id: String,
    val state: String,
    val attributes: HAAttributes,
    val last_changed: String,
    val last_updated: String
)

@JsonClass(generateAdapter = true)
data class HAAttributes(
    @Json(name = "items")
    val items: List<TodoItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HAServiceRequest(
    val entity_id: String,
    val item: String,
    val status: String? = null
)
