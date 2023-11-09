package ru.telecor.gm.mobile.droid.ui.garbageload.fragment.message

import android.os.Build
import ru.telecor.gm.mobile.droid.R
import ru.telecor.gm.mobile.droid.ui.base.BaseBottomSheetFragment

class MessageErrorFragment : BaseBottomSheetFragment() {

    override val layoutRes = R.layout.fragment_message_error

   override fun getTheme(): Int {
       return if (Build.VERSION.SDK_INT > 22) {
           R.style.AppBottomSheetDialogTheme
       } else {
           0
       }
   }
}