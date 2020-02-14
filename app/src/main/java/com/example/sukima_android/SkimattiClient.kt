package com.example.sukima_android

import com.example.sukima_android.model.*
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

    @Headers("Content-Type: application/json")
    @POST("/users/{user_id}/visited")
    suspend fun postVisited(
        @Path("user_id") user_id:Int,
        @Body visited: visited_data?
    ): Visit_res
}

interface RouteClient {
    @GET("/maps/api/directions/json")
    suspend fun getRoute(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") key: String
    ): Route
}