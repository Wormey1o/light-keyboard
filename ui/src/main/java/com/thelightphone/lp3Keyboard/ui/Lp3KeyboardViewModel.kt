package com.thelightphone.lp3Keyboard.ui

import kotlinx.coroutines.flow.StateFlow

interface Lp3KeyboardViewModel : Lp3KeyboardCallback {
    val layoutFlow: StateFlow<Layout>
    val optionsFlow: StateFlow<KeyboardOptions>
}

val defaultEmojis = listOf(
    "😅",
    "😅",
    "🙃",
    "😍",
    "😜",
    "😂",
    "😭",
    "😎",
    "🙌",
    "👍",
    "👎",
    "🤞",
    "✌️",
    "👌",
    "👋",
    "🙏",
    "✨",
    "🔥",
    "❤️",
    "💔",
    "🏆",
    "🎯",
    "👑",
    "👀"
).map { it.codePointAt(0) }

enum class CapsMode { Off, Single, Locked }
