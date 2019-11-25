package com.example.sukima_android

import com.example.sukima_android.model.Spots
import retrofit2.http.GET
import retrofit2.http.Query

interface SkimattiClient {
    @GET("/spots")
    suspend fun getSpot(
        @Query("skima_time") skimaTime: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("genre") genre: String? = null
    ): Spots
}