package id.homebase.homebasekmppoc.di

import id.homebase.homebasekmppoc.lib.youauth.OdinClientFactory
import id.homebase.homebasekmppoc.prototype.lib.drives.files.DriveFileProvider
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youauth.YouAuthProvider
import id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample.apiModule
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.DriveUploadProvider
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.FileDetailViewModel
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.DriveFetchViewModel
import id.homebase.homebasekmppoc.prototype.ui.driveUpload.DriveUploadService
import id.homebase.homebasekmppoc.prototype.ui.driveUpload.DriveUploadViewModel
import id.homebase.homebasekmppoc.ui.screens.home.HomeViewModel
import id.homebase.homebasekmppoc.ui.screens.login.LoginViewModel
import kotlin.uuid.Uuid
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Main application DI module. Register all dependencies here.
 */
val appModule = module {
    includes(
        apiModule
    )

    /* ───────────────────────────
     * Core / Auth
     * ─────────────────────────── */

    // OdinClient - nullable if no stored credentials
    single<OdinClient?> {
        OdinClientFactory.createFromStorage()
    }

    // YouAuthProvider - created with OdinClient
    factory { (odinClient: OdinClient) ->
        YouAuthProvider(odinClient)
    }

    // Main auth flow manager
    singleOf(::YouAuthFlowManager)

    /* ───────────────────────────
     * Drive Providers
     * ─────────────────────────── */

    factory<DriveQueryProvider?> {
        val odinClient: OdinClient? = get()
        odinClient?.let { DriveQueryProvider(it) }
    }

    factory<DriveUploadProvider?> {
        val odinClient: OdinClient? = get()
        odinClient?.let { DriveUploadProvider(it) }
    }

    factory<DriveFileProvider?> {
        val odinClient: OdinClient? = get()
        odinClient?.let { DriveFileProvider(it) }
    }

    /* ───────────────────────────
     * Services
     * ─────────────────────────── */

    single { OdinClientFactory }

    factory<DriveUploadService?> {
        val provider: DriveUploadProvider? = get()
        provider?.let { DriveUploadService(it) }
    }

    /* ───────────────────────────
     * ViewModels
     * ─────────────────────────── */

    // ViewModels
    viewModel {
        LoginViewModel(
            youAuthFlowManager = get(),
            odinClientFactory = get()
        )
    }
    viewModelOf(::DriveFetchViewModel)
    viewModelOf(::HomeViewModel)

    viewModel {
        DriveUploadViewModel(
            getOrNull<DriveUploadService>()
        )
    }

    // 🔑 FileDetailViewModel WITH PARAMETERS
    viewModel { (driveId: Uuid, fileId: Uuid) ->
        FileDetailViewModel(
            driveId = driveId,
            fileId = fileId,
            driveFileProvider = getOrNull<DriveFileProvider>()
        )
    }
}

/**
 * All Koin modules for the application.
 */
val allModules = listOf(
    appModule
)
