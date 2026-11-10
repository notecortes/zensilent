package com.example.model

import android.media.AudioManager
import androidx.compose.ui.graphics.Color
import com.example.R

enum class AudioMode(
    val titleRes: Int,
    val iconRes: Int,
    val ringerMode: Int,
    val isDnd: Boolean = false
) {
    NORMAL(
        titleRes = R.string.profile_normal,
        iconRes = R.drawable.ic_volume_up,
        ringerMode = AudioManager.RINGER_MODE_NORMAL
    ),
    VIBRATE(
        titleRes = R.string.profile_vibrate,
        iconRes = R.drawable.ic_vibration,
        ringerMode = AudioManager.RINGER_MODE_VIBRATE
    ),
    SILENT(
        titleRes = R.string.profile_silent,
        iconRes = R.drawable.ic_volume_off,
        ringerMode = AudioManager.RINGER_MODE_SILENT
    ),
    DND(
        titleRes = R.string.profile_dnd,
        iconRes = R.drawable.ic_do_not_disturb,
        ringerMode = AudioManager.RINGER_MODE_SILENT,
        isDnd = true
    );

    fun next(): AudioMode {
        return when (this) {
            NORMAL -> VIBRATE
            VIBRATE -> SILENT
            SILENT -> DND
            DND -> NORMAL
        }
    }
}
