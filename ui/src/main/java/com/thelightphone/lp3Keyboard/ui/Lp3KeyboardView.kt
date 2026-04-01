package com.thelightphone.lp3Keyboard.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView

class Lp3KeyboardView(context: Context) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        Lp3Keyboard(
            LowerCaseLayout,
            KeyboardOptions(
                emptyList(),
                displayClose = true,
                displayReturn = true
            )
        )
    }
}
