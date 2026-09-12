

package com.biikkkuuuu.muzi.ui.utils

import androidx.navigation.NavController
import com.biikkkuuuu.muzi.ui.screens.Screens

fun NavController.backToMain() {
    val mainRoutes = Screens.MainScreens.map { it.route }

    while (previousBackStackEntry != null &&
        currentBackStackEntry?.destination?.route !in mainRoutes
    ) {
        popBackStack()
    }
}
