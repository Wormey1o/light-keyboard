package com.thelightphone.lp3Keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Lp3Keyboard(layout: Layout, options: KeyboardOptions) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(Color.Black)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            with(layout) { Render(options) }
        }
    }
}

@Composable
fun RowScope.Key(char: Char, onPress: () -> Unit, onRelease: () -> Unit, onLongPress: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .pointerInput(char) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    },
                    onLongPress = { onLongPress() },
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.toString(),
            color = Color.White,
            fontFamily = akkuratFamily,
            fontWeight = FontWeight.Normal,
        )
    }
}

typealias Emoji = Int

data class KeyboardOptions(
    val emojis: List<Emoji>?,
    val displayClose: Boolean,
    val displayReturn: Boolean
)

sealed interface Layout {
    @Composable
    fun ColumnScope.Render(options: KeyboardOptions)
}

object LowerCaseLayout : Layout {

    @Composable
    override fun ColumnScope.Render(options: KeyboardOptions) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
        ) {
            for (char in "qwertyuiop") {
                Key(
                    char,
                    onPress = { println("press $char") },
                    onRelease = { println("release $char") },
                    onLongPress = { println("longpress $char") },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
        ) {
            for (char in "asdfghjkl") {
                Key(
                    char,
                    onPress = { println("press $char") },
                    onRelease = { println("release $char") },
                    onLongPress = { println("longpress $char") },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
        ) {
            for (char in "zxcvbnm") {
                Key(
                    char,
                    onPress = { println("press $char") },
                    onRelease = { println("release $char") },
                    onLongPress = { println("longpress $char") },
                )
            }
        }
    }
}

@Preview(widthDp = (1080 / 4), heightDp = (1240 / 4))
@Composable
fun Lp3KeyboardPreview() {
    Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
        Lp3Keyboard(LowerCaseLayout, KeyboardOptions(emptyList(), true, true))
    }
}