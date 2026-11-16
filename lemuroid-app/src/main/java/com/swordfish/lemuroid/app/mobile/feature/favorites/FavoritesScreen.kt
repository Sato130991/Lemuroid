package com.swordfish.lemuroid.app.mobile.feature.favorites

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
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameListRow
import com.swordfish.lemuroid.lib.library.CollectionsManager
import com.swordfish.lemuroid.lib.library.db.entity.Game

private enum class LibraryOrganizationTab {
    FAVORITES,
    COLLECTIONS,
    FOLDERS,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    val context = LocalContext.current
    val collectionsManager = remember(context) { CollectionsManager(context) }

    val allGames by viewModel.allGames.collectAsState(emptyList())
    val favoriteGames by viewModel.favoriteGamesList.collectAsState(emptyList())
    val collectionNames by collectionsManager.observeCollectionNames().collectAsState(
        CollectionsManager.DEFAULT_COLLECTIONS.toList(),
    )

    var selectedTab by remember { mutableStateOf(LibraryOrganizationTab.FAVORITES) }
    var selectedCollection by remember { mutableStateOf(CollectionsManager.DEFAULT_COLLECTIONS.first()) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var useGridView by remember { mutableStateOf(true) }

    val foldersMap =
        remember(allGames) {
            allGames.groupBy { CollectionsManager.extractFolderName(it) }
                .toSortedMap()
        }

    val folderNames = remember(foldersMap) { foldersMap.keys.toList() }

    val selectedCollectionIds by collectionsManager
        .observeGameIdsInCollection(selectedCollection)
        .collectAsState(emptySet())

    val displayedGames =
        remember(
            selectedTab,
            favoriteGames,
            allGames,
            selectedCollection,
            selectedCollectionIds,
            selectedFolder,
            foldersMap,
        ) {
            when (selectedTab) {
                LibraryOrganizationTab.FAVORITES -> favoriteGames
                LibraryOrganizationTab.COLLECTIONS -> {
                    val ids = collectionsManager.getGameIdsForCollection(selectedCollection) + selectedCollectionIds
                    allGames.filter { it.id in ids }
                }
                LibraryOrganizationTab.FOLDERS -> {
                    val activeFolder = selectedFolder ?: folderNames.firstOrNull()
                    if (activeFolder != null) {
                        foldersMap[activeFolder] ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        // Primary organization tabs + Grid/List toggle
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
                FilterChip(
                    selected = selectedTab == LibraryOrganizationTab.FAVORITES,
                    onClick = { selectedTab = LibraryOrganizationTab.FAVORITES },
                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Favoritos (${favoriteGames.size})") },
                )
                FilterChip(
                    selected = selectedTab == LibraryOrganizationTab.COLLECTIONS,
                    onClick = { selectedTab = LibraryOrganizationTab.COLLECTIONS },
                    leadingIcon = { Icon(Icons.Default.CollectionsBookmark, contentDescription = null) },
                    label = { Text("Colecciones") },
                )
                FilterChip(
                    selected = selectedTab == LibraryOrganizationTab.FOLDERS,
                    onClick = {
                        selectedTab = LibraryOrganizationTab.FOLDERS
                        if (selectedFolder == null) {
                            selectedFolder = folderNames.firstOrNull()
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Carpetas (${folderNames.size})") },
                )
            }

            IconButton(onClick = { useGridView = !useGridView }) {
                Icon(
                    imageVector = if (useGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Cambiar vista",
                )
            }
        }

        // Secondary chips for Collections or Folders
        if (selectedTab == LibraryOrganizationTab.COLLECTIONS) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                collectionNames.forEach { name ->
                    val count = collectionsManager.getGameIdsForCollection(name).size
                    FilterChip(
                        selected = selectedCollection == name,
                        onClick = { selectedCollection = name },
                        label = { Text("$name ($count)") },
                    )
                }
            }
        } else if (selectedTab == LibraryOrganizationTab.FOLDERS && folderNames.isNotEmpty()) {
            val activeFolder = selectedFolder ?: folderNames.firstOrNull()
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                folderNames.forEach { folder ->
                    val count = foldersMap[folder]?.size ?: 0
                    FilterChip(
                        selected = activeFolder == folder,
                        onClick = { selectedFolder = folder },
                        label = { Text("$folder ($count)") },
                    )
                }
            }
        }

        if (displayedGames.isEmpty()) {
            LemuroidEmptyView()
            return@Column
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
                        onFavoriteToggle = { },
                    )
                }
            }
        }
    }
}
