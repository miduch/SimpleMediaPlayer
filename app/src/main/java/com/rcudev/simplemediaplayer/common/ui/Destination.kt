package com.rcudev.simplemediaplayer.common.ui

sealed class Destination(val route: String) {
    data object Main: Destination("main")
    data object Secondary: Destination("secondary")
}