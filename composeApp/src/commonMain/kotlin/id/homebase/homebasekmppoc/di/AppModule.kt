package id.homebase.homebasekmppoc.di

import id.homebase.homebasekmppoc.lib.youauth.OdinClientFactory
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youauth.YouAuthProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.DriveUploadProvider
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.ui.driveUpload.DriveUploadService
import id.homebase.homebasekmppoc.prototype.ui.driveUpload.DriveUploadViewModel
import id.homebase.homebasekmppoc.ui.screens.home.HomeViewModel
import id.homebase.homebasekmppoc.ui.screens.login.LoginViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
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
    // OdinClient - nullable if no stored credentials
    single<OdinClient?> { OdinClientFactory.createFromStorage() }

    // YouAuthProvider - factory that creates new instance with OdinClient
    factory { (odinClient: OdinClient) -> YouAuthProvider(odinClient) }

    // DriveQueryProvider - factory that creates instance with OdinClient (null-safe)
    factory<DriveQueryProvider?> {
        val odinClient: OdinClient? = get()
        odinClient?.let { DriveQueryProvider(it) }
    }

    // DriveUploadProvider - factory that creates instance with OdinClient (null-safe)
    factory<DriveUploadProvider?> {
        val odinClient: OdinClient? = get()
        odinClient?.let { DriveUploadProvider(it) }
    }

    // DriveUploadService - factory that creates instance with DriveUploadProvider (null-safe)
    factory<DriveUploadService?> {
        val provider: DriveUploadProvider? = get()
        provider?.let { DriveUploadService(it) }
    }

    // YouAuthFlowManager - the main auth flow manager for UI
    singleOf(::YouAuthFlowManager)

    // ViewModels
    viewModel { LoginViewModel(get()) }
    viewModelOf(::HomeViewModel)

    // DriveUploadViewModel - factory with nullable DriveUploadService
    viewModel { DriveUploadViewModel(getOrNull<DriveUploadService>()) }
}

/**
 * List of all Koin modules for the application. Add new modules here when creating feature-specific
 * modules.
 */
val allModules = listOf(appModule)
