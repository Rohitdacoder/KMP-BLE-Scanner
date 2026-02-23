package com.example.blekmpapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform