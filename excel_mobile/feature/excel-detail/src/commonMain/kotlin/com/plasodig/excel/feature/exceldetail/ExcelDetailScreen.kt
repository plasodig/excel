package com.plasodig.excel.feature.exceldetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.plasodig.excel.core.ads.AdPlacement
import com.plasodig.excel.core.ads.AdSlot
import com.plasodig.excel.core.designsystem.component.ErrorState
import com.plasodig.excel.core.designsystem.component.LoadingState
import com.plasodig.excel.core.domain.model.TutorialStep
import com.plasodig.excel.core.domain.model.RelatedFunction
import com.plasodig.excel.core.domain.model.Excel
import com.plasodig.excel.core.domain.model.ReportReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelDetailScreen(component: ExcelDetailComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.reportToast) {
        state.reportToast?.let {
            snackbarHostState.showSnackbar(it)
            component.onIntent(ExcelDetailIntent.ConsumeReportToast)
        }
    }

    if (state.showReportDialog) {
        ReportDialog(
            isSubmitting = state.isReporting,
            onSubmit = { reason, detail ->
                component.onIntent(ExcelDetailIntent.SubmitReport(reason, detail))
            },
            onDismiss = { component.onIntent(ExcelDetailIntent.DismissReportDialog) },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.excel?.title ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { component.onIntent(ExcelDetailIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { component.onIntent(ExcelDetailIntent.OpenReportDialog) },
                        enabled = state.excel != null,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = "Laporkan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { component.onIntent(ExcelDetailIntent.ToggleFavorite) }) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorit",
                            tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Banner sticky di bawah — selalu visible tanpa harus scroll.
            // navigationBarsPadding handle gesture bar Android modern.
            Box(Modifier.navigationBarsPadding()) {
                AdSlot(placement = AdPlacement.DetailFooterBanner)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(message = state.error!!)
                state.excel != null -> Column(Modifier.fillMaxSize()) {
                    AnimatedVisibility(visible = state.isFetchingDetail) {
                        FetchingBanner()
                    }
                    AnimatedVisibility(visible = state.fetchError != null && !state.isFetchingDetail) {
                        FetchErrorBanner(
                            message = state.fetchError ?: "",
                            onRetry = { component.onIntent(ExcelDetailIntent.Retry) },
                        )
                    }
                    DetailContent(
                        excel = state.excel!!,
                        checkedSteps = state.checkedSteps,
                        isFetching = state.isFetchingDetail,
                        onStepToggle = { component.onIntent(ExcelDetailIntent.ToggleStep(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FetchingBanner() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Filled.CloudDownload, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Memuat detail tutorial…",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Mengambil rumus & langkah dari server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
        }
    }
}

@Composable
private fun FetchErrorBanner(message: String, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Gagal memuat",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            OutlinedButton(onClick = onRetry) {
                Text("Coba lagi")
            }
        }
    }
}

@Composable
private fun DetailContent(
    excel: Excel,
    checkedSteps: Set<Int>,
    isFetching: Boolean,
    onStepToggle: (Int) -> Unit,
) {
    val isSummaryOnly = excel.relatedFunctions.isEmpty()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { HeroImage(excel = excel) }
        if (!isSummaryOnly) {
            item { MetaRow(excel) }
            if (excel.description.isNotBlank()) {
                item { Description(excel.description) }
            }
            item { SectionHeader("Rumus & Fungsi Terkait") }
            items(excel.relatedFunctions.size) { idx -> RelatedFunctionRow(excel.relatedFunctions[idx]) }
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("Langkah-Langkah") }
            items(excel.steps.size) { idx ->
                val step = excel.steps[idx]
                StepRow(
                    step = step,
                    checked = step.order in checkedSteps,
                    onToggle = { onStepToggle(step.order) },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        } else if (!isFetching) {
            item { EmptyDetailPlaceholder() }
        }
    }
}

@Composable
private fun ReportDialog(
    isSubmitting: Boolean,
    onSubmit: (ReportReason, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(ReportReason.Inaccurate) }
    var detail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Laporkan tutorial",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Pilih alasan pelaporan:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ReportReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == reason,
                            onClick = { selected = reason },
                        )
                        Text(
                            text = reason.label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = detail,
                    onValueChange = { if (it.length <= 500) detail = it },
                    placeholder = { Text("Detail tambahan (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    enabled = !isSubmitting,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(selected, detail.trim()) },
                enabled = !isSubmitting,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Mengirim…")
                } else {
                    Text("Kirim Laporan", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Batal")
            }
        },
    )
}

@Composable
private fun HeroImage(excel: Excel) {
    Box {
        val mod = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))

        if (excel.imageUrl.isBlank()) {
            Box(mod) { ImagePlaceholderForDetail(excel.title, excel.category.label, animated = false) }
        } else {
            SubcomposeAsyncImage(
                model = excel.imageUrl,
                contentDescription = excel.title,
                contentScale = ContentScale.Crop,
                modifier = mod,
                loading = { ImagePlaceholderForDetail(excel.title, excel.category.label, animated = true) },
                error = { ImagePlaceholderForDetail(excel.title, excel.category.label, animated = false) },
                success = { SubcomposeAsyncImageContent(modifier = Modifier.fillMaxSize()) },
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        ) {
            Text(
                text = excel.category.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ImagePlaceholderForDetail(title: String, categoryLabel: String, animated: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(scheme.primaryContainer, scheme.secondaryContainer),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (animated) {
                CircularProgressIndicator(
                    color = scheme.onPrimaryContainer,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp),
                )
            } else {
                Icon(
                    Icons.Filled.GridOn,
                    null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (animated) "Memuat gambar…" else categoryLabel,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun EmptyDetailPlaceholder() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Detail belum tersedia. Periksa koneksi lalu coba lagi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetaRow(excel: Excel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetaChip(icon = Icons.Filled.Schedule, text = "${excel.cookingTimeMinutes} mnt")
        if (excel.servings > 0) {
            MetaChip(icon = Icons.Filled.Layers, text = "Excel ${excel.servings}")
        }
        MetaChip(icon = Icons.Filled.LocalFireDepartment, text = excel.difficulty.name)
    }
}

@Composable
private fun MetaChip(icon: ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Description(description: String) {
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionHeader(title: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    }
}

@Composable
private fun RelatedFunctionRow(relatedFunction: RelatedFunction) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
        ) {}
        Spacer(Modifier.width(12.dp))
        Text(
            text = relatedFunction.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${relatedFunction.quantity} ${relatedFunction.unit}".trim(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepRow(step: TutorialStep, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Langkah ${step.order}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = step.instruction,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
