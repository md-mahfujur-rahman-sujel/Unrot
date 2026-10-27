package com.unrot.app.domain.model

enum class AppCategory(val displayName: String) {
    SOCIAL("Social"),
    MESSAGING("Messaging"),
    ENTERTAINMENT("Entertainment"),
    GAMES("Games"),
    PRODUCTIVITY("Productivity"),
    SYSTEM("System"),
    USER("User"),
    OTHER("Other")
}

enum class InstallSource {
    SYSTEM,
    USER
}
