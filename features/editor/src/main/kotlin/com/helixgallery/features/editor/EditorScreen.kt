// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.editor

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.designsystem.component.HelixFloatingActions
import com.helixgallery.core.designsystem.component.HelixFloatingBackButton
import com.helixgallery.core.designsystem.component.HelixFloatingCircle
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixLoading

private enum class Tab { Crop, Adjust, Filters }

/** Editor: girar, espelhar, cortar, ajustar e filtros. Salva uma cópia; o original não muda. */
@Composable
fun EditorScreen(
    uri: String,
    displayName: String,
    relativePath: String,
    takenMs: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: EditorViewModel = viewModel()
    val context = LocalContext.current
    val load by viewModel.load.collectAsStateWithLifecycle()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.Crop) }
    androidx.compose.runtime.LaunchedEffect(uri) { viewModel.open(uri) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when (load) {
            EditorLoad.Loading -> HelixLoading()
            EditorLoad.Failed -> Text(
                stringResource(R.string.editor_load_failed),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
            EditorLoad.Ready -> {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding().padding(top = 64.dp, bottom = 8.dp)) {
                        preview?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Tab.entries.forEach { t ->
                                    FilterChip(
                                        selected = tab == t,
                                        onClick = { tab = t },
                                        label = { Text(stringResource(tabLabel(t))) },
                                    )
                                }
                                Box(Modifier.weight(1f))
                                if (!params.isUnchanged) {
                                    TextButton(onClick = viewModel::reset) { Text(stringResource(R.string.editor_reset)) }
                                }
                            }
                            when (tab) {
                                Tab.Crop -> CropControls(params, viewModel::update)
                                Tab.Adjust -> AdjustControls(params, viewModel::update)
                                Tab.Filters -> FilterControls(params, viewModel::update)
                            }
                        }
                    }
                }
            }
        }
        HelixFloatingBackButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart))
        if (load == EditorLoad.Ready) {
            HelixFloatingActions(Modifier.align(Alignment.TopEnd)) {
                HelixFloatingCircle {
                    IconButton(
                        enabled = !saving && !params.isUnchanged,
                        onClick = {
                            viewModel.save(displayName, relativePath, takenMs) { ok ->
                                Toast.makeText(
                                    context,
                                    if (ok) R.string.editor_saved else R.string.editor_failed,
                                    Toast.LENGTH_LONG,
                                ).show()
                                if (ok) onClose()
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.editor_save))
                    }
                }
            }
        }
    }
}

private fun tabLabel(tab: Tab): Int = when (tab) {
    Tab.Crop -> R.string.editor_crop
    Tab.Adjust -> R.string.editor_adjust
    Tab.Filters -> R.string.editor_filters
}

private fun aspectLabel(aspect: CropAspect): Int = when (aspect) {
    CropAspect.Original -> R.string.editor_aspect_original
    CropAspect.Square -> R.string.editor_aspect_square
    CropAspect.FourThree -> R.string.editor_aspect_4_3
    CropAspect.ThreeFour -> R.string.editor_aspect_3_4
    CropAspect.SixteenNine -> R.string.editor_aspect_16_9
}

private fun filterLabel(filter: EditFilter): Int = when (filter) {
    EditFilter.None -> R.string.editor_filter_none
    EditFilter.Mono -> R.string.editor_filter_mono
    EditFilter.Sepia -> R.string.editor_filter_sepia
    EditFilter.Vivid -> R.string.editor_filter_vivid
    EditFilter.Cool -> R.string.editor_filter_cool
    EditFilter.Warm -> R.string.editor_filter_warm
    EditFilter.Fade -> R.string.editor_filter_fade
}

@Composable
private fun CropControls(params: EditParams, update: ((EditParams) -> EditParams) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CropAspect.entries.forEach { aspect ->
            FilterChip(
                selected = params.aspect == aspect,
                onClick = { update { it.copy(aspect = aspect) } },
                label = { Text(stringResource(aspectLabel(aspect))) },
            )
        }
    }
    LabeledSlider(
        label = stringResource(R.string.editor_zoom),
        value = params.zoom,
        range = 1f..3f,
        onChange = { v -> update { it.copy(zoom = v) } },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = { update { it.copy(rotationSteps = (it.rotationSteps + 3) % 4) } }) {
            Icon(HelixIcons.RotateLeft, contentDescription = stringResource(R.string.editor_rotate_left))
        }
        IconButton(onClick = { update { it.copy(rotationSteps = (it.rotationSteps + 1) % 4) } }) {
            Icon(HelixIcons.RotateRight, contentDescription = stringResource(R.string.editor_rotate_right))
        }
        IconButton(onClick = { update { it.copy(flipHorizontal = !it.flipHorizontal) } }) {
            Icon(HelixIcons.Flip, contentDescription = stringResource(R.string.editor_flip))
        }
    }
}

@Composable
private fun AdjustControls(params: EditParams, update: ((EditParams) -> EditParams) -> Unit) {
    LabeledSlider(stringResource(R.string.editor_brightness), params.brightness, -0.5f..0.5f) { v -> update { it.copy(brightness = v) } }
    LabeledSlider(stringResource(R.string.editor_contrast), params.contrast, 0.5f..1.5f) { v -> update { it.copy(contrast = v) } }
    LabeledSlider(stringResource(R.string.editor_saturation), params.saturation, 0f..2f) { v -> update { it.copy(saturation = v) } }
}

@Composable
private fun FilterControls(params: EditParams, update: ((EditParams) -> EditParams) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EditFilter.entries.forEach { filter ->
            FilterChip(
                selected = params.filter == filter,
                onClick = { update { it.copy(filter = filter) } },
                label = { Text(stringResource(filterLabel(filter))) },
            )
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp).fillMaxWidth(0.28f))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}
