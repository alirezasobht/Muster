package app.muster

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform