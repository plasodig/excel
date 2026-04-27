package com.plasodig.excel.feature.favorites

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.model.ExcelSummary
import com.plasodig.excel.core.domain.usecase.ObserveFavoritesUseCase
import com.plasodig.excel.core.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class FavoritesState(
    val isLoading: Boolean = true,
    val items: List<ExcelSummary> = emptyList(),
)

sealed interface FavoritesIntent {
    data class Remove(val id: ExcelId) : FavoritesIntent
    data class ItemClicked(val id: ExcelId) : FavoritesIntent
}

interface FavoritesComponent {
    val state: Value<FavoritesState>
    fun onIntent(intent: FavoritesIntent)
}

class DefaultFavoritesComponent(
    componentContext: ComponentContext,
    private val observeFavorites: ObserveFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val onExcelSelected: (ExcelId) -> Unit,
) : FavoritesComponent, ComponentContext by componentContext {

    private val _state = MutableValue(FavoritesState())
    override val state: Value<FavoritesState> = _state
    private val scope = coroutineScope(Dispatchers.Main)

    init {
        observeFavorites()
            .onEach { list ->
                _state.value = FavoritesState(isLoading = false, items = list)
            }
            .launchIn(scope)
    }

    override fun onIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.Remove -> scope.launch { toggleFavorite(intent.id) }
            is FavoritesIntent.ItemClicked -> onExcelSelected(intent.id)
        }
    }
}
