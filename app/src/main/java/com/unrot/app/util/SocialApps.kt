package com.unrot.app.util

object SocialApps {
    val KNOWN_PACKAGES = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",       // TikTok
        "com.ss.android.ugc.trill",       // TikTok alt
        "com.google.android.youtube",
        "com.twitter.android",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.facebook.katana",
        "com.facebook.orca",              // Messenger
        "com.pinterest",
        "com.linkedin.android",
        "com.discord",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.instagram.barcelona",        // Threads
        "tv.twitch.android.app",
        "com.spotify.music"
    )

    fun isSocialApp(packageName: String): Boolean = KNOWN_PACKAGES.contains(packageName)
}
