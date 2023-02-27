package com.wmsoftware.trainingtimer.model

@kotlinx.serialization.Serializable
data class Profile(
    val id: String,
    val name: String,
    val roundTime: Int,
    val breakTime: Int,
    val rounds: Int,
    val roundSeconds: Int,
    val roundMinutes: Int,
    val breakSeconds: Int,
    val breakMinutes: Int
)
