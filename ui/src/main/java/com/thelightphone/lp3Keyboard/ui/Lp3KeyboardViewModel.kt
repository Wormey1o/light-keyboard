package com.thelightphone.lp3Keyboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

interface Lp3RepeatableKeyboardCallback : Lp3KeyboardCallback {
    fun onKeyRepeated(code: Int)
    fun onSpecialKeyRepeated(specialKey: SpecialKey)
}

class DefaultLp3KeyboardViewModel(
    private val delegateCallback: Lp3RepeatableKeyboardCallback,
    private val haptic: () -> Unit = {}
) : ViewModel(),
    Lp3KeyboardViewModel {
    var previousLayout: Layout? = null
        private set

    override val layoutFlow: MutableStateFlow<Layout> = MutableStateFlow(LowerCaseLayout)

    private fun setLayout(layout: Layout) {
        previousLayout = layoutFlow.value
        layoutFlow.value = layout
    }

    override val optionsFlow: StateFlow<KeyboardOptions> = MutableStateFlow(
        KeyboardOptions(
            defaultEmojis,
            displayClose = true,
            displayReturn = true,
            displayVoice = true
        )
    )

    companion object {
        private const val REPEAT_INTERVAL_MS = 500L
    }

    private val heldSpecialKeys = mutableMapOf<SpecialKey, Job>()
    private val heldKeys = mutableMapOf<Int, Job>()

    fun cancelHeldKeys() {
        heldSpecialKeys.values.forEach { it.cancel() }
        heldSpecialKeys.clear()
        heldKeys.values.forEach { it.cancel() }
        heldKeys.clear()
    }
    var capsMode: CapsMode = CapsMode.Off
        private set

    private fun showAlphabetLayout() {
        setLayout(
            when (capsMode) {
                CapsMode.Off -> LowerCaseLayout
                CapsMode.Single -> UpperCaseLayout
                CapsMode.Locked -> CapsLockedLayout
            }
        )
    }

    override fun onKeyPressed(code: Int) {
        haptic()
        // eagerly drop single-caps so fast typists see lowercase before the IME round-trip
        if (capsMode == CapsMode.Single) {
            capsMode = CapsMode.Off
            showAlphabetLayout()
        }
        delegateCallback.onKeyPressed(code)
    }

    override fun onSpecialKeyPressed(key: SpecialKey) {
        haptic()
        delegateCallback.onSpecialKeyPressed(key)
    }

    override fun onKeyReleased(code: Int) {
        heldKeys.remove(code)?.apply {
            cancel()
            return // swallow on key released if held
        }
        // auto-dismiss when a special key is typed
        if (layoutFlow.value is ExtendedCharKeyboard) {
            setLayout(previousLayout ?: LowerCaseLayout)
        }
        delegateCallback.onKeyReleased(code)
    }

    override fun onSpecialKeyReleased(key: SpecialKey) {
        val repeatJob = heldSpecialKeys.remove(key)
        // if we were long-pressing, swallow the release
        repeatJob?.apply {
            cancel()
            return
        }
        var consumed = true
        when (key) {
            SpecialKey.UpCase, SpecialKey.DownCase -> {
                capsMode = when (capsMode) {
                    CapsMode.Off -> CapsMode.Single
                    CapsMode.Single, CapsMode.Locked -> CapsMode.Off
                }
                showAlphabetLayout()
            }

            SpecialKey.Numbers -> {
                setLayout(NumberLayout)
            }

            SpecialKey.Letters -> {
                showAlphabetLayout()
            }

            SpecialKey.Symbols -> {
                setLayout(SymbolsLayout)
            }

            SpecialKey.Emojis -> {
                setLayout(EmojiLayout)
            }

            SpecialKey.Close -> {
                if (!layoutFlow.value.isRootLayout) {
                    showAlphabetLayout()
                } else {
                    consumed = false
                }
            }

            else -> {
                consumed = false
            }
        }
        if (!consumed) {
            delegateCallback.onSpecialKeyReleased(key)
        }
    }

    /** Called by IME after each character to handle system-requested caps. */
    fun setCapsMode(enabled: Boolean) {
        if (capsMode == CapsMode.Locked) return
        capsMode = if (enabled) CapsMode.Single else CapsMode.Off
        when (layoutFlow.value) {
            // only update the layout if we were already showing letters
            LowerCaseLayout, UpperCaseLayout, CapsLockedLayout -> showAlphabetLayout()
            else -> {}
        }
    }

    override fun onKeyLongPressed(code: Int) {
        heldKeys[code]?.cancel()
        if (extendedCharMapping.containsKey(code)) {
            haptic()
            setLayout(ExtendedCharKeyboard(code))
            heldKeys[code] = viewModelScope.launch { }
            return
        }
        delegateCallback.onKeyLongPressed(code)
        heldKeys[code] = viewModelScope.launch {
            while (isActive) {
                delay(REPEAT_INTERVAL_MS)
                delegateCallback.onKeyRepeated(code)
            }
        }
    }

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        heldSpecialKeys[key]?.cancel()
        val allowRepeats = when (key) {
            SpecialKey.UpCase, SpecialKey.DownCase -> {
                capsMode = if (capsMode == CapsMode.Locked) CapsMode.Off else CapsMode.Locked
                heldSpecialKeys[key] = viewModelScope.launch { }
                showAlphabetLayout()
                // don't allow repeats since we switched layouts and the original button is gone
                false
            }

            else -> true
        }
        haptic()
        delegateCallback.onSpecialKeyLongPressed(key)
        if (allowRepeats) {
            heldSpecialKeys[key] = viewModelScope.launch {
                while (isActive) {
                    delay(REPEAT_INTERVAL_MS)
                    delegateCallback.onSpecialKeyRepeated(key)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelHeldKeys()
    }
}
