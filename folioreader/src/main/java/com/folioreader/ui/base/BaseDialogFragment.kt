package com.folioreader.ui.base

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.CallSuper
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import com.folioreader.R

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {


    protected open val fullscreen: Boolean = true
    protected open val transparent: Boolean = false

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding!!

//    protected abstract val viewModel: VM


    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (fullscreen) setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        else setStyle(STYLE_NORMAL, R.style.Theme_AppCompat_Light_Dialog)
    }


    @CallSuper
    override fun onStart() {
        super.onStart()
        if (fullscreen) {
            dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        }

        if (transparent) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    protected fun onBackPressed() {
        requireActivity().onBackPressed()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = initializeViewBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected abstract fun initializeViewBinding(inflater: LayoutInflater, parent: ViewGroup?): VB
}