package com.jhon.mihealthreader.ui.nav

object Routes {
    const val TODAY = "today"
    const val CHARTS = "charts"
    const val SETTINGS = "settings"
}

fun showsBottomBar(route: String?): Boolean =
    route == Routes.TODAY || route == Routes.CHARTS || route == Routes.SETTINGS
