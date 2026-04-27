package com.plasodig.excel.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.plasodig.excel.core.designsystem.component.ExcelCard
import com.plasodig.excel.core.domain.model.GenerationRequestStatus
import com.plasodig.excel.core.domain.model.ExcelSuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(component: SearchComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbarHost.showSnackbar(it)
            component.onIntent(SearchIntent.ConsumeToast)
        }
    }

    state.rateLimitDialog?.let { dialog ->
        RateLimitDialog(
            state = dialog,
            onWatchAd = { component.onIntent(SearchIntent.WatchRewardedAd) },
            onDismiss = { component.onIntent(SearchIntent.DismissRateLimit) },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Cari Tutorial") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            QueryInput(state = state, onIntent = component::onIntent)
            Box(Modifier.fillMaxSize()) {
                when {
                    state.isSearching -> SearchingIndicator()
                    state.error != null -> ErrorBox(state.error!!)
                    state.results.isNotEmpty() -> ResultList(state, component::onIntent)
                    state.isEmpty -> EmptyWithSuggestions(state = state, onIntent = component::onIntent)
                    else -> EmptyHint()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueryInput(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { onIntent(SearchIntent.QueryChanged(it)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onIntent(SearchIntent.Clear) }) {
                        Icon(Icons.Filled.Close, "Hapus")
                    }
                }
            },
            placeholder = { Text("Mis. VLOOKUP, PivotTable, IF bersarang…") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EmptyHint() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Cari tutorial berdasarkan nama",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Kalau tutorial belum tersedia, kamu bisa langsung minta dibuatkan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun SearchingIndicator() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
    }
}

@Composable
private fun EmptyWithSuggestions(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Tidak ada tutorial cocok dengan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "\"${state.query}\"",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Suggestions (kalau ada yang similar di katalog).
        when {
            state.isFetchingSuggestions && state.generate == null -> item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Mencari saran…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.suggestions.isNotEmpty() && state.generate == null -> {
                item {
                    Text(
                        text = "Mungkin yang kamu maksud:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                items(items = state.suggestions, key = { it.id.value }) { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        onClick = { onIntent(SearchIntent.ItemClicked(suggestion.id)) },
                    )
                }
            }
        }

        // CTA generate atau banner status.
        item {
            when (val gen = state.generate) {
                null -> GenerateCta(state = state, onIntent = onIntent)
                else -> GenerateCard(gen = gen, query = state.query, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: ExcelSuggestion, onClick: () -> Unit) {
    ExcelCard(
        title = suggestion.title,
        description = suggestion.description.ifBlank { "Sentuh untuk lihat detail" },
        imageUrl = suggestion.imageUrl,
        category = suggestion.category.label,
        difficulty = suggestion.difficulty.name,
        cookingTimeMinutes = suggestion.cookingTimeMinutes,
        isFavorite = false,
        onClick = onClick,
        onFavoriteClick = {},
        compact = false,
    )
}

@Composable
private fun GenerateCta(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "Belum ada — ingin kami buatkan?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Tutorial \"${state.query}\" akan ditambahkan ke katalog kamu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onIntent(SearchIntent.GenerateExcel) },
                enabled = state.query.trim().length >= 3,
            ) {
                Text("Buat tutorial ini")
            }
        }
    }
}

@Composable
private fun GenerateCard(gen: GenerateJob, query: String, onIntent: (SearchIntent) -> Unit) {
    when (gen.status) {
        GenerationRequestStatus.Processing,
        GenerationRequestStatus.Unknown -> ProcessingCard(gen = gen, query = query, onIntent = onIntent)
        GenerationRequestStatus.Completed,
        GenerationRequestStatus.AlreadyExists -> CompletedCard(gen = gen, onIntent = onIntent)
        GenerationRequestStatus.Failed -> FailedCard(gen = gen, onIntent = onIntent)
    }
}

@Composable
private fun ProcessingCard(gen: GenerateJob, query: String, onIntent: (SearchIntent) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Sedang membuat \"$query\"",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = gen.message.ifBlank { "Sedang menyiapkan rumus, langkah, dan gambar. Biasanya 15-30 detik." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Kamu boleh tinggal halaman ini — tutorial akan tetap tersimpan.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CompletedCard(gen: GenerateJob, onIntent: (SearchIntent) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "Tutorial berhasil dibuat",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = gen.message.ifBlank { "Tutorial sudah masuk ke katalog." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onIntent(SearchIntent.OpenGeneratedExcel) },
                enabled = gen.excelId != null,
            ) {
                Text("Buka tutorial")
            }
        }
    }
}

@Composable
private fun FailedCard(gen: GenerateJob, onIntent: (SearchIntent) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "Gagal membuat tutorial",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = gen.errorMessage ?: gen.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onIntent(SearchIntent.DismissGenerate) }) {
                    Text("Tutup")
                }
                Button(onClick = { onIntent(SearchIntent.GenerateExcel) }) {
                    Text("Coba lagi")
                }
            }
        }
    }
}

@Composable
private fun ErrorBox(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Pencarian gagal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Escape hatch dialog saat user kena rate limit generate. Dua opsi:
 *  - Tunggu (dismiss dialog)
 *  - Tonton iklan → trigger rewarded ad → kalau selesai, server kasih 1 extra generate
 */
@Composable
private fun RateLimitDialog(
    state: RateLimitDialogState,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.isWatchingAd) onDismiss() },
        title = {
            Text(
                text = "Batas harian tercapai",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Kamu sudah membuat 5 tutorial dalam satu jam terakhir. Kamu bisa " +
                        "menunggu, atau tonton iklan singkat untuk mendapat 1 tutorial tambahan sekarang.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onWatchAd,
                enabled = !state.isWatchingAd,
            ) {
                if (state.isWatchingAd) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Memuat iklan…")
                } else {
                    Text("Tonton iklan", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isWatchingAd) {
                Text("Tunggu")
            }
        },
    )
}

@Composable
private fun ResultList(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = state.results, key = { it.id.value }) { item ->
            ExcelCard(
                title = item.title,
                description = item.description.ifBlank { "Sentuh untuk lihat detail" },
                imageUrl = item.imageUrl,
                category = item.category.label,
                difficulty = item.difficulty.name,
                cookingTimeMinutes = item.cookingTimeMinutes,
                isFavorite = item.isFavorite,
                onClick = { onIntent(SearchIntent.ItemClicked(item.id)) },
                onFavoriteClick = { onIntent(SearchIntent.ToggleFavorite(item.id)) },
                compact = false,
            )
        }
    }
}
