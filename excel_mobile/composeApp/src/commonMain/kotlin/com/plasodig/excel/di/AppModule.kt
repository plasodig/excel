package com.plasodig.excel.di

import com.plasodig.excel.core.data.di.dataModule
import com.plasodig.excel.core.data.di.useCaseModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun commonModules(): List<Module> = listOf(
    dataModule,
    useCaseModule,
)

fun initKoin(additionalConfig: KoinAppDeclaration = {}): KoinApplication = startKoin {
    additionalConfig()
    modules(commonModules())
}
