// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.helixgallery.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.helixgallery.core.model.MediaItem
import com.helixgallery.core.model.MediaState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

internal class MediaStoreRepository(private val context: Context) : MediaRepository {

    private val resolver: ContentResolver = context.contentResolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val filesUri: Uri = MediaStore.Files.getContentUri("external")

    private fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        // O MediaStore avisa pelas URIs de imagens e vídeos (a das "files" sozinha não pega foto nova de outro app).
        val watched = listOf(
            filesUri,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
        watched.forEach { resolver.registerContentObserver(it, true, observer) }
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    override val state: StateFlow<MediaState> = changes()
        .debounce(400)
        .onStart { emit(Unit) }
        .mapLatest { load() }
        .flowOn(Dispatchers.IO)
        // Lazily: depois do primeiro uso a lista fica viva (e o monitoramento ligado) até o app fechar;
        // ao voltar de outro app a lista já está pronta, sem recarregar nem piscar.
        .stateIn(scope, SharingStarted.Lazily, MediaState())

    private fun load(): MediaState {
        val active = query(trashed = false).sortedByDescending { it.takenMs }
        val trashed = query(trashed = true).sortedByDescending { it.takenMs }
        return MediaState(items = active, trashed = trashed, loaded = true)
    }

    private fun query(trashed: Boolean): List<MediaItem> {
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)",
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                ),
            )
            putInt(
                MediaStore.QUERY_ARG_MATCH_TRASHED,
                if (trashed) MediaStore.MATCH_ONLY else MediaStore.MATCH_EXCLUDE,
            )
        }
        val result = ArrayList<MediaItem>()
        try {
            resolver.query(filesUri, PROJECTION, args, null)?.use { cursor ->
                val cId = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val cName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val cMime = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val cType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val cTaken = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val cAdded = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val cWidth = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val cHeight = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val cDuration = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
                val cSize = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val cBucketId = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val cBucket = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val cPath = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val cFav = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cId)
                    val isVideo = cursor.getInt(cType) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val base = if (isVideo) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val addedMs = cursor.getLong(cAdded) * 1000L
                    val taken = cursor.getLong(cTaken)
                    result += MediaItem(
                        id = id,
                        uri = ContentUris.withAppendedId(base, id).toString(),
                        isVideo = isVideo,
                        displayName = cursor.getString(cName).orEmpty(),
                        mimeType = cursor.getString(cMime).orEmpty(),
                        takenMs = if (taken > 0) taken else addedMs,
                        addedMs = addedMs,
                        width = cursor.getInt(cWidth),
                        height = cursor.getInt(cHeight),
                        durationMs = cursor.getLong(cDuration),
                        sizeBytes = cursor.getLong(cSize),
                        bucketId = cursor.getLong(cBucketId),
                        bucketName = cursor.getString(cBucket).orEmpty(),
                        relativePath = cursor.getString(cPath).orEmpty(),
                        isFavorite = cursor.getInt(cFav) == 1,
                    )
                }
            }
        } catch (_: SecurityException) {
            // Sem permissão (ainda): lista vazia; a tela de acesso cuida de pedir.
        }
        return result
    }

    override fun trashRequest(uris: List<String>, trash: Boolean): IntentSender =
        MediaStore.createTrashRequest(resolver, uris.map(Uri::parse), trash).intentSender

    override fun deleteRequest(uris: List<String>): IntentSender =
        MediaStore.createDeleteRequest(resolver, uris.map(Uri::parse)).intentSender

    override fun favoriteRequest(uris: List<String>, favorite: Boolean): IntentSender =
        MediaStore.createFavoriteRequest(resolver, uris.map(Uri::parse), favorite).intentSender

    override fun canManageMedia(): Boolean = MediaStore.canManageMedia(context)

    private companion object {
        val PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.IS_FAVORITE,
        )
    }
}
