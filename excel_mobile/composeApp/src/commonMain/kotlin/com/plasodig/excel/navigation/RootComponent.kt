package com.plasodig.excel.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.feature.favorites.DefaultFavoritesComponent
import com.plasodig.excel.feature.favorites.FavoritesComponent
import com.plasodig.excel.feature.exceldetail.DefaultExcelDetailComponent
import com.plasodig.excel.feature.exceldetail.ExcelDetailComponent
import com.plasodig.excel.feature.excellist.DefaultExcelListComponent
import com.plasodig.excel.feature.excellist.ExcelListComponent
import com.plasodig.excel.feature.search.DefaultSearchComponent
import com.plasodig.excel.feature.search.SearchComponent
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun onTabSelected(tab: Tab)
    fun onBack()

    enum class Tab { List, Search, Favorites }

    sealed interface Child {
        data class ExcelList(val component: ExcelListComponent) : Child
        data class Search(val component: SearchComponent) : Child
        data class Favorites(val component: FavoritesComponent) : Child
        data class ExcelDetail(val component: ExcelDetailComponent) : Child
    }
}

@OptIn(DelicateDecomposeApi::class)
class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.ExcelList,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    override fun onTabSelected(tab: RootComponent.Tab) {
        val target = when (tab) {
            RootComponent.Tab.List -> Config.ExcelList
            RootComponent.Tab.Search -> Config.Search
            RootComponent.Tab.Favorites -> Config.Favorites
        }
        navigation.bringToFront(target)
    }

    override fun onBack() {
        navigation.pop()
    }

    private fun createChild(config: Config, ctx: ComponentContext): RootComponent.Child = when (config) {
        Config.ExcelList -> RootComponent.Child.ExcelList(
            DefaultExcelListComponent(
                componentContext = ctx,
                getExcels = get(),
                toggleFavorite = get(),
                refresh = get(),
                loadPage = get(),
                onExcelSelected = { navigation.push(Config.ExcelDetail(it.value)) },
            ),
        )
        Config.Search -> RootComponent.Child.Search(
            DefaultSearchComponent(
                componentContext = ctx,
                search = get(),
                fetchSuggestions = get(),
                submitRequest = get(),
                submitRequestExtra = get(),
                fetchRequestStatus = get(),
                toggleFavorite = get(),
                rewardedController = get(),
                onExcelSelected = { navigation.push(Config.ExcelDetail(it.value)) },
            ),
        )
        Config.Favorites -> RootComponent.Child.Favorites(
            DefaultFavoritesComponent(
                componentContext = ctx,
                observeFavorites = get(),
                toggleFavorite = get(),
                onExcelSelected = { navigation.push(Config.ExcelDetail(it.value)) },
            ),
        )
        is Config.ExcelDetail -> RootComponent.Child.ExcelDetail(
            DefaultExcelDetailComponent(
                componentContext = ctx,
                excelId = ExcelId(config.id),
                getDetail = get(),
                toggleFavorite = get(),
                ensureDetail = get(),
                reportExcel = get(),
                onBack = { navigation.pop() },
            ),
        )
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object ExcelList : Config
        @Serializable data object Search : Config
        @Serializable data object Favorites : Config
        @Serializable data class ExcelDetail(val id: String) : Config
    }
}
