package id.homebase.homebasekmppoc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform