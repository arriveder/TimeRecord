package com.example.timerecord.network.dto

import com.google.gson.annotations.SerializedName

data class SendCodeRequest(
    @SerializedName("email") val email: String,
    @SerializedName("type") val type: String
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("password") val password: String
)

data class LoginRequest(
    @SerializedName("usernameOrEmail") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("user") val user: UserDto?
) {
    data class UserDto(
        @SerializedName("id") val id: String,
        @SerializedName("username") val username: String,
        @SerializedName("email") val email: String
    )
}
