package id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val apiModule = module {
    single { HttpClientProvider.create() }
    singleOf(::CredentialsManager)
    singleOf(::AuthRepository)
    singleOf(::ApiService)
}

