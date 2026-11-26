// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.albums

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.core.datastore.HelixPreferences
import com.helixgallery.core.model.AlbumTool
import com.helixgallery.core.model.DEFAULT_ALBUM_TOOLS
import com.helixgallery.core.model.MediaState
import com.helixgallery.core.model.moveAlbumTool
import com.helixgallery.data.media.MediaData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumsViewModel(application: Application) : AndroidViewModel(application) {
    val media: StateFlow<MediaState> = MediaData.repository(application).state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaState())

    private val prefs = HelixPreferences.repository(application)

    /** Ferramentas fixadas no topo, na ordem em que a pessoa deixou. */
    val tools: StateFlow<List<AlbumTool>> = prefs.preferences.map { it.albumTools }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_ALBUM_TOOLS)

    fun pin(tool: AlbumTool) = save(tools.value + tool)

    fun unpin(tool: AlbumTool) = save(tools.value - tool)

    fun move(tool: AlbumTool, delta: Int) = save(moveAlbumTool(tools.value, tool, delta))

    private fun save(list: List<AlbumTool>) {
        viewModelScope.launch { prefs.setAlbumTools(list) }
    }
}
