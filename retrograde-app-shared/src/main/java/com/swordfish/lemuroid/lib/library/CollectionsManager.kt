package com.swordfish.lemuroid.lib.library

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionsManager(private val sharedPreferences: SharedPreferences) {
    constructor(context: Context) : this(SharedPreferencesHelper.getSharedPreferences(context))

    private val flowSharedPreferences = FlowSharedPreferences(sharedPreferences)

    fun observeCollectionNames(): Flow<List<String>> {
        return flowSharedPreferences
            .getStringSet(KEY_COLLECTION_NAMES, DEFAULT_COLLECTIONS)
            .asFlow()
            .map { set ->
                (DEFAULT_COLLECTIONS + set).distinct()
            }
    }

    fun getCollectionNames(): List<String> {
        val stored = sharedPreferences.getStringSet(KEY_COLLECTION_NAMES, DEFAULT_COLLECTIONS) ?: DEFAULT_COLLECTIONS
        return (DEFAULT_COLLECTIONS + stored).distinct()
    }

    fun observeCollectionsMap(): Flow<Map<String, Set<Int>>> {
        return observeCollectionNames().map { names ->
            names.associateWith { name -> getGameIdsForCollection(name) }
        }
    }

    fun observeGameIdsInCollection(collectionName: String): Flow<Set<Int>> {
        return flowSharedPreferences
            .getStringSet(collectionKey(collectionName), emptySet())
            .asFlow()
            .map { set -> set.mapNotNull { it.toIntOrNull() }.toSet() }
    }

    fun getGameIdsForCollection(collectionName: String): Set<Int> {
        val raw = sharedPreferences.getStringSet(collectionKey(collectionName), emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun getCollectionsForGame(gameId: Int): Set<String> {
        return getCollectionNames()
            .filter { name -> gameId in getGameIdsForCollection(name) }
            .toSet()
    }

    fun toggleGameInCollection(
        collectionName: String,
        gameId: Int,
    ): Boolean {
        ensureCollectionExists(collectionName)
        val current =
            sharedPreferences
                .getStringSet(collectionKey(collectionName), emptySet())
                ?.toMutableSet() ?: mutableSetOf()
        val idStr = gameId.toString()
        val added =
            if (idStr in current) {
                current.remove(idStr)
                false
            } else {
                current.add(idStr)
                true
            }
        sharedPreferences.edit {
            putStringSet(collectionKey(collectionName), current)
        }
        return added
    }

    fun createCollection(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        ensureCollectionExists(trimmed)
    }

    private fun ensureCollectionExists(name: String) {
        val names =
            sharedPreferences
                .getStringSet(KEY_COLLECTION_NAMES, DEFAULT_COLLECTIONS)
                ?.toMutableSet() ?: DEFAULT_COLLECTIONS.toMutableSet()
        if (name !in names) {
            names.add(name)
            sharedPreferences.edit {
                putStringSet(KEY_COLLECTION_NAMES, names)
            }
        }
    }

    private fun collectionKey(name: String): String = "${KEY_COLLECTION_PREFIX}_$name"

    companion object {
        private const val KEY_COLLECTION_NAMES = "library_collections_names"
        private const val KEY_COLLECTION_PREFIX = "library_collection_items"

        val DEFAULT_COLLECTIONS =
            setOf(
                "Jugando",
                "Pendientes",
                "Completados",
                "Multijugador",
            )

        fun extractFolderName(game: Game): String {
            return runCatching {
                val decoded = Uri.decode(game.fileUri)
                val normalized = decoded.substringAfterLast(':').replace('\\', '/')
                val parts = normalized.split('/').filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    parts[parts.size - 2]
                } else {
                    game.systemId.uppercase()
                }
            }.getOrDefault(game.systemId.uppercase())
        }
    }
}
