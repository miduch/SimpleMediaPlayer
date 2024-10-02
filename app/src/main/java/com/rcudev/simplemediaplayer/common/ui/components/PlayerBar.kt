package com.rcudev.simplemediaplayer.common.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rcudev.simplemediaplayer.common.ui.UIEvent

@Composable
internal fun PlayerBar(
    progress: Float,
    durationString: String,
    progressString: String,
    onUiEvent: (UIEvent) -> Unit
) {
    var newProgressValue by remember { mutableFloatStateOf(0f) }
    var useNewProgressValue by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Slider(
            value = if (useNewProgressValue) newProgressValue else progress,
            onValueChange = { newValue ->
                useNewProgressValue = true
                newProgressValue = newValue
                onUiEvent(UIEvent.UpdateProgress(newProgress = newValue))
            },
            onValueChangeFinished = {
                useNewProgressValue = false
            },
            modifier = Modifier
                .padding(horizontal = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(text = progressString)
            Text(text = durationString)
        }
    }
}
