package com.example.sukima_android.model

data class Spot(
    val comment: String,
    val genre: String,
    val name: String,
    val plan: String,
    val position: Position,
    val spot_id: Int,
    val stay_time: Int
)