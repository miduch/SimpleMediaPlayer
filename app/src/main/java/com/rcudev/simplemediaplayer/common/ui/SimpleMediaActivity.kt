package com.rcudev.simplemediaplayer.common.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.MoreExecutors
import com.rcudev.player_service.service.SimpleMediaService
import com.rcudev.simplemediaplayer.common.ui.theme.SimpleMediaPlayerTheme
import com.rcudev.simplemediaplayer.main.SimpleMediaScreen
import com.rcudev.simplemediaplayer.secondary.SecondaryScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SimpleMediaActivity : ComponentActivity() {

    private val viewModel: SimpleMediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SimpleMediaPlayerTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Destination.Main.route) {
                    composable(Destination.Main.route) {
                        SimpleMediaScreen(
                            vm = viewModel,
                            navController = navController,
                        )
                    }
                    composable(Destination.Secondary.route) {
                        SecondaryScreen(vm = viewModel)
                    }
                }
            }
        }
    }
}