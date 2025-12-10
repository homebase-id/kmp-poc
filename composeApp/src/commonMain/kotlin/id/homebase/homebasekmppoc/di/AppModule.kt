package id.homebase.homebasekmppoc.di

import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import id.homebase.homebasekmppoc.ui.screens.home.HomeViewModel
import id.homebase.homebasekmppoc.ui.screens.login.LoginViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Main application DI module. Register all dependencies here.
 *
 * Usage:
 * - singleOf(::ClassName) - creates a singleton
 * - factoryOf(::ClassName) - creates new instance each time
 * - single { ClassName() } - singleton with custom initialization
 * - factory { ClassName() } - factory with custom initialization
 */
val appModule = module {
    // Authentication
    singleOf(::YouAuthManager)

    // ViewModels
    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModel)

    // Add more dependencies here as needed:
    // singleOf(::SomeRepository)
    // factoryOf(::SomeUseCase)
}

/**
 * List of all Koin modules for the application. Add new modules here when creating feature-specific
 * modules.
 */
val allModules = listOf(appModule)
