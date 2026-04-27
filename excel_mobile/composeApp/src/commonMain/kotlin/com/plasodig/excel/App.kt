package com.plasodig.excel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.plasodig.excel.core.ads.ConsentManager
import com.plasodig.excel.core.designsystem.theme.AppTheme
import com.plasodig.excel.feature.favorites.FavoritesScreen
import com.plasodig.excel.feature.exceldetail.ExcelDetailScreen
import com.plasodig.excel.feature.excellist.ExcelListScreen
import com.plasodig.excel.feature.search.SearchScreen
import com.plasodig.excel.image.SetupExcelImageLoader
import com.plasodig.excel.navigation.RootComponent
import org.koin.compose.koinInject

@Composable
fun App(root: RootComponent) {
    SetupExcelImageLoader()
    val consentManager: ConsentManager = koinInject()
    LaunchedEffect(Unit) {
        // Request UMP consent sekali di startup (aman dipanggil berulang — UMP idempotent).
        consentManager.ensureConsent()
    }
    AppTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            val stack by root.stack.subscribeAsState()
            val active = stack.active.instance
            val showBottomBar = active is RootComponent.Child.ExcelList ||
                active is RootComponent.Child.Search ||
                active is RootComponent.Child.Favorites

            Scaffold(
                // Outer scaffold tidak punya topBar — inner Scaffold (per screen) yang handle status bar.
                // Tanpa override ini, top inset di-double-count → ada gap besar antara status bar dan TopAppBar.
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomBar) {
                        BottomBar(
                            active = active,
                            onTabSelected = root::onTabSelected,
                        )
                    }
                },
            ) { padding ->
                Children(
                    stack = root.stack,
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) { child ->
                    when (val instance = child.instance) {
                        is RootComponent.Child.ExcelList -> ExcelListScreen(instance.component)
                        is RootComponent.Child.Search -> SearchScreen(instance.component)
                        is RootComponent.Child.Favorites -> FavoritesScreen(instance.component)
                        is RootComponent.Child.ExcelDetail -> ExcelDetailScreen(instance.component)
                    }
                }
            }
        }
    }
}

/**
 * Floating pill-style bottom bar. Icon-only untuk tab non-aktif, icon + label untuk tab aktif
 * (animated expand). Height compact ~58 dp, rounded 32 dp, elevated dengan shadow.
 */
@Composable
private fun BottomBar(
    active: RootComponent.Child,
    onTabSelected: (RootComponent.Tab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 6.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 3.dp,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomBarItem(
                    icon = Icons.Filled.GridOn,
                    label = "Tutorial",
                    selected = active is RootComponent.Child.ExcelList,
                    onClick = { onTabSelected(RootComponent.Tab.List) },
                )
                BottomBarItem(
                    icon = Icons.Filled.Search,
                    label = "Cari",
                    selected = active is RootComponent.Child.Search,
                    onClick = { onTabSelected(RootComponent.Tab.Search) },
                )
                BottomBarItem(
                    icon = Icons.Filled.Favorite,
                    label = "Favorit",
                    selected = active is RootComponent.Child.Favorites,
                    onClick = { onTabSelected(RootComponent.Tab.Favorites) },
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220),
        label = "fg",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(200)) + expandHorizontally(tween(220)),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(160)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
