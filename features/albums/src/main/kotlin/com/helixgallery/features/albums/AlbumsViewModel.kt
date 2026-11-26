// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.albums

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.core.model.MediaState
import com.helixgallery.data.media.MediaData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AlbumsViewModel(application: Application) : AndroidViewModel(application) {
    val media: StateFlow<MediaState> = MediaData.repository(application).state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaState())
}
