package com.plasodig.excel.feature.excellist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.plasodig.excel.core.ads.AdPlacement
import com.plasodig.excel.core.ads.AdSlot
import com.plasodig.excel.core.designsystem.component.CategoryChip
import com.plasodig.excel.core.designsystem.component.ErrorState
import com.plasodig.excel.core.designsystem.component.LoadingState
import com.plasodig.excel.core.designsystem.component.ExcelCard
import com.plasodig.excel.core.domain.model.ExcelCategory
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelListScreen(component: ExcelListComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.refreshEvent?.id) {
        val event = state.refreshEvent ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = event.message, duration = SnackbarDuration.Short)
        component.onIntent(ExcelListIntent.ConsumeRefreshEvent)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Excel Indonesia", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                // True hard error — bukan empty list. Tampilkan retry.
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { component.onIntent(ExcelListIntent.Load) },
                )
                // First-time bootstrap (DB belum punya seed). Initial load only.
                state.isLoading && state.items.isEmpty() -> LoadingState()
                // Semua case lain (termasuk empty per kategori): grid + chips + pull-refresh aktif.
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { component.onIntent(ExcelListIntent.Refresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ExcelGrid(state = state, component = component)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcelGrid(state: ExcelListState, component: ExcelListComponent) {
    val gridState = rememberLazyGridState()

    // Auto-load-more saat scroll mendekati akhir. `hasMore` cuts re-trigger setelah server
    // sudah kasih semua; `isPaging` dan `isRefreshing` cuts concurrent fetch.
    LaunchedEffect(gridState, state.items.size, state.loadedCount, state.reachedEnd) {
        snapshotFlow {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = gridState.layoutInfo.totalItemsCount
            last to total
        }
            .distinctUntilChanged()
            .collect { (last, total) ->
                if (state.hasMore && !state.isPaging && !state.isRefreshing &&
                    total > 0 && last >= total - 4
                ) {
                    component.onIntent(ExcelListIntent.LoadMore)
                }
            }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Filter chips — selalu render walau list kosong
        item(span = { GridItemSpan(2) }) {
            CategoryChipStrip(
                selected = state.selectedCategory,
                onSelect = { component.onIntent(ExcelListIntent.FilterChanged(it)) },
            )
        }

        // Body: list, empty, atau footer
        if (state.items.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                CategoryEmptyHint(category = state.selectedCategory)
            }
        } else {
            // Render cards dengan Native Advanced ad disisipkan setiap N item (full-width span).
            // Native ad tampilan mirip ExcelCard (judul, media, CTA) — blend dengan konten,
            // UX jauh lebih baik dari banner. AdMob policy-safe karena ada label "Sponsored".
            val adInterval = 8
            state.items.forEachIndexed { index, item ->
                item(key = item.id.value) {
                    ExcelCard(
                        title = item.title,
                        description = item.description.ifBlank { "Sentuh untuk lihat detail" },
                        imageUrl = item.imageUrl,
                        category = item.category.label,
                        difficulty = item.difficulty.name,
                        cookingTimeMinutes = item.cookingTimeMinutes,
                        isFavorite = item.isFavorite,
                        onClick = { component.onIntent(ExcelListIntent.ItemClicked(item.id)) },
                        onFavoriteClick = { component.onIntent(ExcelListIntent.ToggleFavorite(item.id)) },
                        compact = true,
                    )
                }
                val isDivisor = (index + 1) % adInterval == 0
                if (isDivisor && index < state.items.size - 1) {
                    item(span = { GridItemSpan(2) }, key = "ad_$index") {
                        AdSlot(placement = AdPlacement.ListInFeedNative)
                    }
                }
            }

            if (state.hasMore) {
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = if (state.totalCount > 0) {
                                "Memuat lebih banyak… (${state.items.size}/${state.totalCount})"
                            } else {
                                "Memuat lebih banyak…"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "${state.items.size} tutorial · semua sudah dimuat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipStrip(
    selected: ExcelCategory?,
    onSelect: (ExcelCategory?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            CategoryChip(
                label = "Semua",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(ExcelCategory.entries.toList(), key = { it.name }) { category ->
            CategoryChip(
                label = category.label,
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryEmptyHint(category: ExcelCategory?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Inbox,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (category != null) "Belum ada tutorial di kategori \"${category.label}\""
            else "Belum ada tutorial",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (category != null) "Pilih kategori lain di atas, atau tarik ke bawah untuk memuat ulang."
            else "Tarik ke bawah untuk memuat ulang.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

