package com.thelightphone.lp3keyboard

import android.view.View
import android.view.WindowInsets
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardView

class IMEService : LifecycleInputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    override fun onCreateInputView(): View {
        val view = Lp3KeyboardView(this)
        setCandidatesViewShown(false);
        window?.window?.apply{
            decorView.let { decorView ->
                decorView.setViewTreeLifecycleOwner(this@IMEService)
                decorView.setViewTreeViewModelStoreOwner(this@IMEService)
                decorView.setViewTreeSavedStateRegistryOwner(this@IMEService)
            }
        }
        return view
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
    }

    override val viewModelStore: ViewModelStore
        get() = store
    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private val store = ViewModelStore()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

}