package com.zionhuang.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.constants.CONTENT_TYPE_HEADER
import com.zionhuang.music.constants.CONTENT_TYPE_PLAYLIST
import com.zionhuang.music.constants.GridThumbnailHeight
import com.zionhuang.music.db.entities.Album
import com.zionhuang.music.db.entities.Artist
import com.zionhuang.music.db.entities.Playlist
import com.zionhuang.music.db.entities.PlaylistEntity
import com.zionhuang.music.ui.component.AlbumGridItem
import com.zionhuang.music.ui.component.ArtistGridItem
import com.zionhuang.music.ui.component.LocalMenuState
import com.zionhuang.music.ui.component.PlaylistGridItem
import com.zionhuang.music.ui.menu.AlbumMenu
import com.zionhuang.music.ui.menu.ArtistMenu
import com.zionhuang.music.ui.menu.PlaylistMenu
import com.zionhuang.music.viewmodels.LibraryMixViewModel
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val likedSongs by viewModel.likedSongs.collectAsState()
    val downloadSongs by viewModel.downloadSongs.collectAsState(initial = null)
    val topSongs by viewModel.topSongs.collectAsState(initial = null)
    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val likedPlaylist = Playlist(
        playlist = PlaylistEntity(id = UUID.randomUUID().toString(), name = "Liked"),
        songCount = if (likedSongs != null) likedSongs!!.size else 0,
        thumbnails = emptyList()
    )

    val downloadPlaylist = Playlist(
        playlist = PlaylistEntity(id = UUID.randomUUID().toString(), name = "Offline"),
        songCount = if (downloadSongs!= null) downloadSongs!!.size else 0,
        thumbnails = emptyList()
    )

    val topSizeInt = topSize.toString().toInt()

    val topPlaylist = Playlist(
        playlist = PlaylistEntity(id = UUID.randomUUID().toString(), name = "My Top $topSize"),
        songCount = topSongs?.let { minOf(it.size, topSizeInt) } ?: 0,
        thumbnails = emptyList()
    )

    val albums = viewModel.albums.collectAsState()
    val artist = viewModel.artists.collectAsState()
    val playlist = viewModel.playlists.collectAsState()

    val allItems = albums.value + artist.value + playlist.value

    val coroutineScope = rememberCoroutineScope()

    val lazyGridState = rememberLazyGridState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            item(
                key = "filter",
                span = { GridItemSpan(maxLineSpan) },
                contentType = CONTENT_TYPE_HEADER
            ) {
                filterContent()
            }

            item(
                key = "likedPlaylist",
               contentType = { CONTENT_TYPE_PLAYLIST }
            ) {
                PlaylistGridItem(
                    playlist = likedPlaylist,
                    fillMaxWidth = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                navController.navigate("auto_playlist/liked")
                            },
                            onLongClick = {
                                menuState.show {
                                    PlaylistMenu(
                                        playlist = likedPlaylist,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                        autoPlaylist = true,
                                        songList = likedSongs
                                    )
                                }
                            }
                        )
                        .animateItemPlacement()
                )
            }

            item(
                key = "downloadedPlaylist",
                contentType = { CONTENT_TYPE_PLAYLIST }
            ) {
                PlaylistGridItem(
                    playlist = downloadPlaylist,
                    fillMaxWidth = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                navController.navigate("auto_playlist/downloaded")
                            },
                            onLongClick = {
                                menuState.show {
                                    PlaylistMenu(
                                        playlist = downloadPlaylist,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                        autoPlaylist = true,
                                        downloadPlaylist = true,
                                        songList = downloadSongs
                                    )
                                }
                            }
                        )
                        .animateItemPlacement()
                )
            }

            item(
                key = "TopPlaylist",
                contentType = { CONTENT_TYPE_PLAYLIST }
            ) {
                PlaylistGridItem(
                    playlist = topPlaylist,
                    fillMaxWidth = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                navController.navigate("top_playlist/$topSize")
                            },
                            onLongClick = {
                                menuState.show {
                                    PlaylistMenu(
                                        playlist = topPlaylist,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                        autoPlaylist = true,
                                        songList = topSongs?.subList(
                                            0,
                                            minOf(topSizeInt, topPlaylist.songCount)
                                        )
                                    )
                                }
                            }
                        )
                        .animateItemPlacement()
                )
            }


            items(
                items = allItems,
                key = { it.id },
                contentType = { CONTENT_TYPE_PLAYLIST }
            ) { item ->
                when (item) {
                    is Playlist -> {
                        PlaylistGridItem(
                            playlist = item,
                            fillMaxWidth = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("local_playlist/${item.id}")
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            PlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                .animateItemPlacement()
                        )
                    }
                    is Artist -> {
                        ArtistGridItem(
                            artist = item,
                            fillMaxWidth = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("artist/${item.id}")
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            ArtistMenu(
                                                originalArtist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                .animateItemPlacement()
                        )
                    }
                    is Album -> {
                        AlbumGridItem(
                            album = item,
//                            isActive = item.id == mediaMetadata?.item?.id,
                            isPlaying = isPlaying,
                            coroutineScope = coroutineScope,
                            fillMaxWidth = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("item/${item.id}")
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            AlbumMenu(
                                                originalAlbum = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                .animateItemPlacement()
                        )
                    }
                    else -> {}
                }

            }
        }
    }
}