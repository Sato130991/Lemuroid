package com.swordfish.lemuroid.app.mobile.feature.games

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameListRow
import com.swordfish.lemuroid.lib.library.CollectionsManager
import com.swordfish.lemuroid.lib.library.db.entity.Game

private enum class GamesSortMode(val label: String) {
    NAME("A-Z"),
    RECENT("Recientes"),
    FAVORITES_FIRST("Favoritos"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamesScreen(
    modifier: Modifier = Modifier,
    viewModel: GamesViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
) {
    val allGames by viewModel.gamesListFlow.collectAsState(emptyList())
    var sortMode by remember { mutableStateOf(GamesSortMode.NAME) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var useGridView by remember { mutableStateOf(false) }

    if (allGames.isEmpty()) {
        LemuroidEmptyView()
        return
    }

    val folders =
        remember(allGames) {
            allGames.map { CollectionsManager.extractFolderName(it) }.distinct().sorted()
        }

    val displayedGames =
        remember(allGames, sortMode, selectedFolder) {
            val filtered =
                if (selectedFolder == null) {
                    allGames
                } else {
                    allGames.filter { CollectionsManager.extractFolderName(it) == selectedFolder }
                }

            when (sortMode) {
                GamesSortMode.NAME -> filtered.sortedBy { it.displayName().lowercase() }
                GamesSortMode.RECENT ->
                    filtered.sortedWith(
                        compareByDescending<Game> { it.lastPlayedAt ?: 0L }
                            .thenBy { it.displayName().lowercase() },
                    )
                GamesSortMode.FAVORITES_FIRST ->
                    filtered.sortedWith(
                        compareByDescending<Game> { it.isFavorite }
                            .thenBy { it.displayName().lowercase() },
                    )
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GamesSortMode.values().forEach { mode ->
                    FilterChip(
                        selected = sortMode == mode,
                        onClick = { sortMode = mode },
                        label = { Text(mode.label) },
                    )
                }
                if (folders.size > 1) {
                    folders.forEach { folder ->
                        FilterChip(
                            selected = selectedFolder == folder,
                            onClick = {
                                selectedFolder = if (selectedFolder == folder) null else folder
                            },
                            label = { Text("📁 $folder") },
                        )
                    }
                }
            }

            IconButton(onClick = { useGridView = !useGridView }) {
                Icon(
                    imageVector = if (useGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Cambiar vista",
                )
            }
        }

        if (useGridView) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                columns = GridCells.Adaptive(144.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(displayedGames.size, key = { displayedGames[it].id }) { index ->
                    val game = displayedGames[index]
                    LemuroidGameCard(
                        modifier = Modifier.animateItem(),
                        game = game,
                        onClick = { onGameClick(game) },
                        onLongClick = { onGameLongClick(game) },
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(displayedGames.size, key = { displayedGames[it].id }) { index ->
                    val game = displayedGames[index]
                    LemuroidGameListRow(
                        modifier = Modifier.animateItem(),
                        game = game,
                        onClick = { onGameClick(game) },
                        onLongClick = { onGameLongClick(game) },
                        onFavoriteToggle = { isFavorite -> onGameFavoriteToggle(game, isFavorite) },
                    )
                }
            }
        }
    }
}
