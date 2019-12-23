package com.example.sukima_android

import com.example.sukima_android.model.Spots
import com.example.sukima_android.model.User
import com.example.sukima_android.model.post_user
import retrofit2.http.*

interface SkimattiClient {
    @GET("/spots")
    suspend fun getSpot(
        @Query("skima_time") skimaTime: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("user_id")user_id:Int,
        @Query("genre") genre: String? = null
    ): Spots

    @Headers("Content-Type: application/json")
    @POST("/users")
     suspend fun postUser(
    @Body postUser: post_user
    ):User
}