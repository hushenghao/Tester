package com.dede.tester.ext

import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.dede.tester.R


fun Fragment.findCoordinator(): CoordinatorLayout {
    return requireActivity().findViewById(R.id.coordinator)
}