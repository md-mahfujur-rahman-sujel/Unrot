package com.unrot.app.util

import com.unrot.app.domain.model.AppCategory

object AppCategories {

    private val SOCIAL = setOf(
        "com.instagram.android",
        "com.instagram.barcelona",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.facebook.katana",
        "com.pinterest",
        "com.linkedin.android",
        "com.tinder",
        "com.zhiliaoapp.musically.go"
    )

    private val MESSAGING = setOf(
        "com.whatsapp",
        "com.facebook.orca",
        "org.telegram.messenger",
        "com.discord",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.viber.voip",
        "com.skype.raider",
        "com.slack"
    )

    private val ENTERTAINMENT = setOf(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "tv.twitch.android.app",
        "com.spotify.music",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.zhiliaoapp.musically"
    )

    private val GAMES_HINTS = listOf(
        "game", ".unity3d", "com.supercell", "com.king.", "com.mojang",
        "com.roblox", "com.epicgames", "com.activision", "com.ea."
    )

    fun categorize(packageName: String, isSystem: Boolean, isGame: Boolean = false): AppCategory {
        return when {
            packageName in SOCIAL -> AppCategory.SOCIAL
            packageName in MESSAGING -> AppCategory.MESSAGING
            packageName in ENTERTAINMENT -> AppCategory.ENTERTAINMENT
            isGame || GAMES_HINTS.any { packageName.contains(it, ignoreCase = true) } -> AppCategory.GAMES
            isSystem -> AppCategory.SYSTEM
            SocialApps.isSocialApp(packageName) -> AppCategory.SOCIAL
            else -> AppCategory.USER
        }
    }

    /** Returns true if the package should be treated as a social app for sorting/highlighting,
     * even if its primary category is something else (like Messaging or Entertainment). */
    fun isSocialApp(packageName: String, category: AppCategory): Boolean {
        return category == AppCategory.SOCIAL || SocialApps.isSocialApp(packageName)
    }

    fun groupPackages(category: AppCategory): Set<String> = when (category) {
        AppCategory.SOCIAL -> SOCIAL
        AppCategory.MESSAGING -> MESSAGING
        AppCategory.ENTERTAINMENT -> ENTERTAINMENT
        else -> emptySet()
    }
}
