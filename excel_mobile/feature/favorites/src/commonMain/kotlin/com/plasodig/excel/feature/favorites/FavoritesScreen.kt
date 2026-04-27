package com.plasodig.excel.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.plasodig.excel.core.designsystem.component.EmptyState
import com.plasodig.excel.core.designsystem.component.LoadingState
import com.plasodig.excel.core.designsystem.component.ExcelCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(component: FavoritesComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Tutorial Favorit") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.items.isEmpty() -> EmptyState(
                    title = "Belum ada favorit",
                    subtitle = "Tandai tutorial dengan ikon hati untuk menyimpannya di sini.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.id.value }) { item ->
                        ExcelCard(
                            title = item.title,
                            description = item.description,
                            imageUrl = item.imageUrl,
                            category = item.category.label,
                            difficulty = item.difficulty.name,
                            cookingTimeMinutes = item.cookingTimeMinutes,
                            isFavorite = true,
                            onClick = { component.onIntent(FavoritesIntent.ItemClicked(item.id)) },
                            onFavoriteClick = { component.onIntent(FavoritesIntent.Remove(item.id)) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
