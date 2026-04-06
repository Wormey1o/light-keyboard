package com.thelightphone.lp3Keyboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed interface Layout {
    @Composable
    fun ColumnScope.Render(options: KeyboardOptions, callback: Lp3KeyboardCallback)
}

object LowerCaseLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("qwertyuiop", callback)
        SecondRow("asdfghjkl", callback)
        ThirdRow("zxcvbnm", callback) {
            IconKey(
                R.drawable.up_lp3,
                SpecialKey.UpCase,
                callback,
                width = ICON_KEY_WIDTH_DP.dp,
                modifier = Modifier.padding(12.dp).padding(bottom = 6.dp, end = 8.dp)
            )
        }
        FinalRow(options, callback) {
            MultiLabelKey("123", SpecialKey.Numbers, callback)
        }
    }
}

object CapsLockedLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("QWERTYUIOP", callback)
        SecondRow("ASDFGHJKL", callback)
        ThirdRow("ZXCVBNM", callback) {
            IconKey(
                R.drawable.caps_lp3,
                SpecialKey.DownCase,
                callback,
                width = ICON_KEY_WIDTH_DP.dp,
                modifier = Modifier.padding(9.dp).padding(bottom = 2.dp, end = 4.dp)
            )
        }
        FinalRow(options, callback) {
            MultiLabelKey("123", SpecialKey.Numbers, callback)
        }
    }
}

object UpperCaseLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("QWERTYUIOP", callback)
        SecondRow("ASDFGHJKL", callback)
        ThirdRow("ZXCVBNM", callback) {
            IconKey(
                R.drawable.down_lp3,
                SpecialKey.DownCase,
                callback,
                width = ICON_KEY_WIDTH_DP.dp,
                modifier = Modifier.padding(12.dp).padding(bottom = 6.dp, end = 8.dp)
            )
        }
        FinalRow(options, callback) {
            MultiLabelKey("123", SpecialKey.Numbers, callback)
        }
    }
}

object NumberLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("1234567890", callback)
        SecondRow("-/:;()$&@\"", callback)
        ThirdRow(".,?!'", callback) {
            MultiLabelKey("#+=", SpecialKey.Symbols, callback)
        }
        FinalRow(options, callback) {
            MultiLabelKey("ABC", SpecialKey.Letters, callback)
        }
    }
}

object SymbolsLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("[]{}#%^*+=", callback)
        SecondRow("_\\|~<>€£¥", callback)
        ThirdRow(".,?!'", callback) {
            MultiLabelKey("123", SpecialKey.Numbers, callback)
        }
        FinalRow(options, callback) {
            MultiLabelKey("ABC", SpecialKey.Letters, callback)
        }
    }
}

object EmojiLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        // current layout supports 3 rows of 8
        val emojiRows = options.emojis?.chunked(8)?.take(3) ?: return
        for (row in emojiRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(STANDARD_ROW_HEIGHT_DP.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                for (emoji in row) {
                    Key(emoji, callback, width = MEDIUM_KEY_WIDTH_DP.dp)
                }
            }
        }
    }
}