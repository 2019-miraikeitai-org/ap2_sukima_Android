package com.example.sukima_android.model

import java.io.Serializable

data class User (
    val user_id : Int
)

data class post_user(
    val age : Int,
    val gender : String
) : Serializable