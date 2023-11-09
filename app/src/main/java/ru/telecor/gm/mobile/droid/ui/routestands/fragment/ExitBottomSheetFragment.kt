package ru.telecor.gm.mobile.droid.ui.routestands.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_exit_bottom_sheet.*
import ru.telecor.gm.mobile.droid.R

class ExitBottomSheetFragment(var listener: Listener) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_exit_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sureExitBtn.setOnClickListener {
            listener.setOnClickYes()
            dismiss()
        }

        returnExitBtn.setOnClickListener {
            dismiss()
        }
    }

 override fun getTheme(): Int {
    return if(Build.VERSION.SDK_INT > 22){
        R.style.AppBottomSheetDialogTheme
    }else{
        0
    }
}

    interface Listener{
        fun setOnClickYes()
    }
}