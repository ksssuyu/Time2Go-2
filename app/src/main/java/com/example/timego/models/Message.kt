package com.example.timego.models

import com.google.firebase.Timestamp

data class Message(
    val messageId: String = "",
    val conversationId: String = "",
    val userId: String? = null,
    val text: String = "",
    val type: String = "user", // “user”, “bot”, “system”
    val createdAt: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val routes: List<Route> = emptyList(), // Маршруты в сообщении
    val attachments: List<Map<String, Any>> = emptyList()
    )
