package com.thelightphone.lp3Keyboard.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import com.thelightphone.lp3Keyboard.ui.layout.EmojiLayout
import com.thelightphone.lp3Keyboard.ui.layout.Layout
import com.thelightphone.lp3Keyboard.ui.layout.SwipeConfig
import com.thelightphone.lp3Keyboard.ui.layout.UpperCaseLayout
import com.thelightphone.lp3Keyboard.ui.viewmodel.defaultEmojis
import kotlinx.coroutines.flow.first

enum class SpecialKey {
    UpCase,
    DownCase,
    Backspace,
    Space,
    Letters,
    Numbers,
    Symbols,
    Emojis,
    Submit,
    Close,
    Voice,
    Return
}

interface Lp3KeyboardCallback {
    fun onKeyPressed(code: Int)
    fun onSpecialKeyPressed(key: SpecialKey)
    fun onKeyReleased(code: Int)
    fun onSpecialKeyReleased(key: SpecialKey)
    fun onKeyLongPressed(code: Int)
    fun onSpecialKeyLongPressed(key: SpecialKey)
    fun onSubmitWord(word: CharSequence)

    // Pointer left the key bounds before lifting. Clean up / do not treat as tap
    fun onKeyCancelled(code: Int) = onKeyReleased(code)
}

interface Lp3KeyboardSwipeCallback<ResultType> {
    fun onSwipeLayoutReady(letters: String, cx: FloatArray, cy: FloatArray) = Unit
    fun onSwipeStarted() = Unit
    fun onSwipeCompleted(x: FloatArray, y: FloatArray, t: FloatArray): List<ResultType> =
        emptyList()
    fun getWordForResult(swipeResult: ResultType): CharSequence? = null
}

const val LP3_KEYBOARD_HEIGHT_DP = 164
const val STANDARD_KEY_WIDTH_DP = 35
const val ICON_KEY_WIDTH_DP = STANDARD_KEY_WIDTH_DP + 14
const val MEDIUM_KEY_WIDTH_DP = STANDARD_KEY_WIDTH_DP + 8
const val STANDARD_ROW_HEIGHT_DP = 44
const val STANDARD_KEY_TEXT_SP = 25
const val MINIMUM_SWIPE_DP = 40

@Composable
fun Lp3Keyboard(
    layout: Layout,
    options: KeyboardOptions,
    callback: Lp3KeyboardCallback,
    swipeCallback: Lp3KeyboardSwipeCallback<*>?
) {
    val swipeConfig = layout.swipeConfig.takeIf { options.swipeEnabled }
    // Pointer positions inside the swipe gesture are local to this Box, but the
    // letter bounds reported via onGloballyPositioned/boundsInRoot are in the
    // composition root's coordinate space. Track the Box's own root offset so the
    // swipe handler can reconcile them — needed whenever the keyboard isn't
    // pinned at the root origin (e.g. embedded above other UI).
    val boxRootOffset = remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(LP3_KEYBOARD_HEIGHT_DP.dp)
            .background(LocalKeyboardColors.current.background)
            .onGloballyPositioned { boxRootOffset.value = it.positionInRoot() }
            .then(
                if (swipeConfig != null) {
                    Modifier.pointerInput(swipeConfig) {
                        val minSwipePx = MINIMUM_SWIPE_DP.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startTime = down.uptimeMillis
                            val xs = ArrayList<Float>()
                            val ys = ArrayList<Float>()
                            val ts = ArrayList<Float>()
                            xs.add(down.position.x)
                            ys.add(down.position.y)
                            ts.add(0f)
                            var minX = down.position.x
                            var maxX = down.position.x
                            var minY = down.position.y
                            var maxY = down.position.y
                            var swipeStarted = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: break
                                val p = change.position
                                xs.add(p.x); ys.add(p.y)
                                ts.add((change.uptimeMillis - startTime).toFloat())
                                if (p.x < minX) minX = p.x
                                if (p.x > maxX) maxX = p.x
                                if (p.y < minY) minY = p.y
                                if (p.y > maxY) maxY = p.y
                                if (swipeCallback != null && !swipeStarted) {
                                    val displacementPx = maxOf(maxX - minX, maxY - minY)
                                    if (displacementPx >= minSwipePx) {
                                        swipeCallback.onSwipeStarted()
                                        swipeStarted = true
                                    }
                                }
                                if (!change.pressed) break
                            }

                            if (swipeCallback == null) return@awaitEachGesture
                            val finalDisplacement = maxOf(maxX - minX, maxY - minY)
                            if (finalDisplacement < minSwipePx) return@awaitEachGesture
                            val rect = swipeConfig.letterBoundsRect() ?: return@awaitEachGesture
                            val w = rect.width.coerceAtLeast(1f)
                            val h = rect.height.coerceAtLeast(1f)
                            // Lift Box-local touch coordinates into root space before
                            // normalizing against the root-space letter rect.
                            val ox = boxRootOffset.value.x
                            val oy = boxRootOffset.value.y
                            val nx = FloatArray(xs.size) { (xs[it] + ox - rect.left) / w }
                            val ny = FloatArray(ys.size) { (ys[it] + oy - rect.top) / h }
                            val nt = FloatArray(ts.size) { ts[it] }
                            swipeCallback.onSwipeCompleted(nx, ny, nt)
                        }
                    }
                } else Modifier
            )
    ) {
        Column(Modifier.fillMaxSize().padding(top = 4.dp).align(Alignment.Center)) {
            with(layout) { Render(options, callback) }
        }
    }
    if (swipeConfig != null) {
        LaunchedEffect(swipeConfig) {
            swipeConfig.boundsFlow.first()
            swipeConfig.deriveLayout()?.let { (letters, cx, cy) ->
                swipeCallback?.onSwipeLayoutReady(letters, cx, cy)
            }
        }
    }
}

fun Modifier.keyInput(
    inputKey: Any?,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    onLongPressed: () -> Unit,
    onPressedChanged: (Boolean) -> Unit,
    onCancelled: () -> Unit = onReleased
) = pointerInput(inputKey) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false).also { it.consume() }
        onPressedChanged(true)
        onPressed()
        // waitForUpOrCancellation returns null when the pointer leaves our
        // bounds. It was a drag vs. a tap. Track which one so callers
        // can suppress the IME commit while still cleaning up press state.
        var up: PointerInputChange? = null
        try {
            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                up = waitForUpOrCancellation()?.also { it.consume() }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            onLongPressed()
            up = waitForUpOrCancellation()?.also { it.consume() }
        }
        onPressedChanged(false)
        if (up != null) onReleased() else onCancelled()
    }
}

@Composable
fun RowScope.IconKey(
    @DrawableRes drawable: Int,
    key: SpecialKey,
    callback: Lp3KeyboardCallback,
    enableKeyAnimation: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = STANDARD_KEY_WIDTH_DP.dp
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .keyInput(
                inputKey = key,
                onPressed = { callback.onSpecialKeyPressed(key) },
                onReleased = { callback.onSpecialKeyReleased(key) },
                onLongPressed = { callback.onSpecialKeyLongPressed(key) },
                onPressedChanged = { pressed = it }
            )
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(drawable),
            contentDescription = "TODO",
            tint = LocalKeyboardColors.current.foreground,
            modifier = Modifier.then(
                if (enableKeyAnimation) {
                    Modifier.graphicsLayer {
                        val isPressed = pressed
                        scaleX = if (isPressed) 1.25f else 1f
                        scaleY = if (isPressed) 1.25f else 1f
                        translationY = if (isPressed) -12.dp.toPx() else 0f
                    }
                } else {
                    Modifier
                }
            )
        )
    }
}


@Composable
fun RowScope.SpaceBar(callback: Lp3KeyboardCallback, width: Dp, enableKeyAnimation: Boolean) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxHeight()
            .width(width)
            .padding(bottom = 6.dp)
            .keyInput(
                inputKey = Unit,
                onPressed = { callback.onSpecialKeyPressed(SpecialKey.Space) },
                onReleased = { callback.onSpecialKeyReleased(SpecialKey.Space) },
                onLongPressed = { callback.onSpecialKeyLongPressed(SpecialKey.Space) },
                onPressedChanged = { pressed = it }
            ).then(
                if (enableKeyAnimation) {
                    Modifier.graphicsLayer {
                        val isPressed = pressed
                        scaleX = if (isPressed) 1.1f else 1f
                        scaleY = if (isPressed) 1.1f else 1f
                        translationY = if (isPressed) -8.dp.toPx() else 0f
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            Modifier
                .height(2.dp)
                .background(LocalKeyboardColors.current.foreground)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun RowScope.Key(
    char: Char,
    callback: Lp3KeyboardCallback,
    swipeConfig: SwipeConfig?,
    enableKeyAnimation: Boolean,
    override: SpecialKey? = null
) = Key(char.code, callback, swipeConfig, enableKeyAnimation, override)

@Composable
fun RowScope.Key(
    code: Int,
    callback: Lp3KeyboardCallback,
    swipeConfig: SwipeConfig?,
    enableKeyAnimation: Boolean,
    override: SpecialKey? = null,
    width: Dp = STANDARD_KEY_WIDTH_DP.dp
) {
    var pressed by remember { mutableStateOf(false) }

    val onPressed = override
        ?.let { { callback.onSpecialKeyPressed(it) } }
        ?: { callback.onKeyPressed(code) }

    val onReleased = override
        ?.let { { callback.onSpecialKeyReleased(it) } }
        ?: { callback.onKeyReleased(code) }

    val onLongPressed = override
        ?.let { { callback.onSpecialKeyLongPressed(it) } }
        ?: { callback.onKeyLongPressed(code) }

    // Drag-off (pointer leaves the key bounds): for letter keys this is the
    // start of a potential swipe — route to onKeyCancelled so the IME doesn't
    // commit the character. Special-key overrides keep release semantics.
    val onCancelled = override
        ?.let { { callback.onSpecialKeyReleased(it) } }
        ?: { callback.onKeyCancelled(code) }

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .then(
                if (swipeConfig != null && override == null) {
                    Modifier.onGloballyPositioned { swipeConfig.report(code, it.boundsInRoot()) }
                } else Modifier
            )
            .keyInput(
                inputKey = code,
                onPressed = onPressed,
                onReleased = onReleased,
                onLongPressed = onLongPressed,
                onPressedChanged = { pressed = it },
                onCancelled = onCancelled
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildString { appendCodePoint(code) },
            color = LocalKeyboardColors.current.foreground,
            fontFamily = akkuratFamily,
            fontWeight = FontWeight.Normal,
            fontSize = STANDARD_KEY_TEXT_SP.sp,
            modifier = Modifier.then(
                if (enableKeyAnimation) {
                    Modifier.graphicsLayer {
                        val isPressed = pressed
                        scaleX = if (isPressed) 1.25f else 1f
                        scaleY = if (isPressed) 1.25f else 1f
                        translationY = if (isPressed) -12.dp.toPx() else 0f
                    }
                } else {
                    Modifier
                }
            )
        )
    }
}

@Composable
fun RowScope.MultiLabelKey(
    labelText: String,
    key: SpecialKey,
    callback: Lp3KeyboardCallback,
    enableKeyAnimation: Boolean
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(ICON_KEY_WIDTH_DP.dp)
            .fillMaxHeight()
            .keyInput(
                inputKey = labelText,
                onPressed = { callback.onSpecialKeyPressed(key) },
                onReleased = { callback.onSpecialKeyReleased(key) },
                onLongPressed = { callback.onSpecialKeyLongPressed(key) },
                onPressedChanged = { pressed = it }
            ),
        contentAlignment = BiasAlignment(-0.2f, 0.2f)
    ) {
        Text(
            text = labelText,
            color = LocalKeyboardColors.current.foreground,
            fontFamily = akkuratFamily,
            fontWeight = FontWeight.Normal,
            letterSpacing = 2.sp,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.then(
                if (enableKeyAnimation) {
                    Modifier.graphicsLayer {
                        val isPressed = pressed  // state read happens at draw time
                        scaleX = if (isPressed) 1.25f else 1f
                        scaleY = if (isPressed) 1.25f else 1f
                        translationY = if (isPressed) -12.dp.toPx() else 0f
                    }
                } else {
                    Modifier
                }
            )
        )
    }
}

typealias Emoji = Int

data class KeyboardOptions(
    val emojis: List<Emoji>?,
    val displayReturn: Boolean,
    val displayVoice: Boolean,
    val enableKeyAnimation: Boolean,
    val swipeEnabled: Boolean
)

data class LayoutOptions(
    val displayCloseButton: Boolean
)

@Composable
fun ColumnScope.DefaultRow(
    height: Dp = STANDARD_ROW_HEIGHT_DP.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}


@Composable
fun ColumnScope.FirstRow(
    characters: String,
    callback: Lp3KeyboardCallback,
    swipeConfig: SwipeConfig?,
    enableKeyAnimation: Boolean
) {
    DefaultRow {
        for (char in characters) {
            Key(char, callback, swipeConfig, enableKeyAnimation)
        }
    }
}

@Composable
fun ColumnScope.SecondRow(
    characters: String,
    callback: Lp3KeyboardCallback,
    swipeConfig: SwipeConfig?,
    enableKeyAnimation: Boolean
) {
    // same style as first row on all keyboards
    FirstRow(characters, callback, swipeConfig, enableKeyAnimation)
}

@Composable
fun ColumnScope.ThirdRow(
    characters: String,
    callback: Lp3KeyboardCallback,
    swipeConfig: SwipeConfig?,
    keyboardOptions: KeyboardOptions,
    leftButton: @Composable RowScope.() -> Unit
) {
    DefaultRow {
        leftButton()
        if (characters.length == 5) {
            // currently this row only has 5 or 7 chars, so add some space if there are 5
            Spacer(Modifier.width(MEDIUM_KEY_WIDTH_DP.dp))
        }
        for (char in characters) {
            Key(char, callback, swipeConfig, keyboardOptions.enableKeyAnimation)
        }
        if (characters.length == 5) {
            Spacer(Modifier.width(STANDARD_KEY_WIDTH_DP.dp))
        }
        IconKey(
            R.drawable.back_lp3,
            SpecialKey.Backspace,
            callback,
            keyboardOptions.enableKeyAnimation,
            width = ICON_KEY_WIDTH_DP.dp,
            modifier = Modifier.padding(10.dp).padding(start = 8.dp, bottom = 6.dp)
        )
    }
}

@Composable
fun ColumnScope.FinalRow(
    options: KeyboardOptions,
    callback: Lp3KeyboardCallback,
    leftButton: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .height((STANDARD_ROW_HEIGHT_DP - 20).dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        val iconKeyWidth = STANDARD_KEY_WIDTH_DP + 12
        leftButton()
        if (!options.emojis.isNullOrEmpty()) {
            IconKey(
                R.drawable.smile,
                SpecialKey.Emojis,
                callback,
                options.enableKeyAnimation,
                width = iconKeyWidth.dp,
                modifier = Modifier.padding(start = 5.dp, end = 6.5.dp).padding(end = 16.dp)
            )
        } else {
            Spacer(Modifier.width(iconKeyWidth.dp))
        }
        SpaceBar(callback, 160.dp, options.enableKeyAnimation)
        if (options.displayReturn) {
            IconKey(
                R.drawable.return_lp3,
                SpecialKey.Return,
                callback,
                options.enableKeyAnimation,
                width = iconKeyWidth.dp,
                modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 0.dp)
            )
        } else {
            Spacer(Modifier.width(iconKeyWidth.dp))
        }

        if (options.displayVoice) {
            IconKey(
                R.drawable.microphone_lp3,
                SpecialKey.Voice,
                callback,
                options.enableKeyAnimation,
                width = iconKeyWidth.dp,
                modifier = Modifier.padding(top = 2.dp, start = 12.dp, end = 4.dp)
            )
        } else {
            Spacer(Modifier.width(iconKeyWidth.dp))
        }
    }
}

internal val previewCallback = object : Lp3KeyboardCallback {
    override fun onKeyPressed(code: Int) = Unit
    override fun onSpecialKeyPressed(key: SpecialKey) = Unit
    override fun onKeyReleased(code: Int) = Unit
    override fun onSpecialKeyReleased(key: SpecialKey) = Unit
    override fun onKeyLongPressed(code: Int) = Unit
    override fun onSpecialKeyLongPressed(key: SpecialKey) = Unit
    override fun onSubmitWord(word: CharSequence) = Unit
}

@Preview(name = "Dark", widthDp = (1080 / 3), heightDp = (1240 / 3))
@Composable
fun Lp3KeyboardDarkPreview() {
    Lp3KeyboardTheme(DarkKeyboardColors) {
        Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
            val keyboardOptions = KeyboardOptions(
                defaultEmojis,
                displayReturn = true,
                displayVoice = true,
                enableKeyAnimation = true,
                swipeEnabled = true
            )
            val layoutOptions = LayoutOptions(displayCloseButton = true)
            Lp3KeyboardWrapper(
                EmojiLayout,
                keyboardOptions,
                layoutOptions,
                previewCallback,
                null
            )
        }
    }
}

@Preview(name = "Light", widthDp = (1080 / 3), heightDp = (1240 / 3))
@Composable
fun Lp3KeyboardLightPreview() {
    Lp3KeyboardTheme(LightKeyboardColors) {
        Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
            val keyboardOptions = KeyboardOptions(
                defaultEmojis,
                displayReturn = true,
                displayVoice = true,
                enableKeyAnimation = true,
                swipeEnabled = true
            )
            val layoutOptions = LayoutOptions(displayCloseButton = true)
            Lp3KeyboardWrapper(
                UpperCaseLayout,
                keyboardOptions,
                layoutOptions,
                previewCallback,
                null
            )
        }
    }
}