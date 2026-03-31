package com.example.timerecord.network

import com.example.timerecord.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth endpoints
    @POST("api/auth/send-code")
    suspend fun sendVerificationCode(@Body request: SendCodeRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<AuthResponse>>

    // Record endpoints
    @GET("api/records")
    suspend fun getRecords(@Header("Authorization") token: String): Response<ApiResponse<List<RecordResponse>>>

    @GET("api/records/by-date")
    suspend fun getRecordsByDate(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): Response<ApiResponse<List<RecordResponse>>>

    @GET("api/records/{id}")
    suspend fun getRecordById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ApiResponse<RecordResponse>>

    @POST("api/records")
    suspend fun createRecord(
        @Header("Authorization") token: String,
        @Body request: RecordRequest
    ): Response<ApiResponse<RecordResponse>>

    @PUT("api/records/{id}")
    suspend fun updateRecord(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: RecordRequest
    ): Response<ApiResponse<RecordResponse>>

    @DELETE("api/records/{id}")
    suspend fun deleteRecord(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    // Label endpoints
    @GET("api/labels")
    suspend fun getLabels(@Header("Authorization") token: String): Response<ApiResponse<List<LabelResponse>>>

    @GET("api/labels/{id}")
    suspend fun getLabelById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ApiResponse<LabelResponse>>

    @POST("api/labels")
    suspend fun createLabel(
        @Header("Authorization") token: String,
        @Body request: LabelRequest
    ): Response<ApiResponse<LabelResponse>>

    @PUT("api/labels/{id}")
    suspend fun updateLabel(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: LabelRequest
    ): Response<ApiResponse<LabelResponse>>

    @DELETE("api/labels/{id}")
    suspend fun deleteLabel(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    // Sync endpoints
    @POST("api/sync/records")
    suspend fun syncRecords(
        @Header("Authorization") token: String,
        @Body request: SyncRequest
    ): Response<ApiResponse<RecordSyncResponse>>

    @POST("api/sync/labels")
    suspend fun syncLabels(
        @Header("Authorization") token: String,
        @Body request: SyncRequest
    ): Response<ApiResponse<LabelSyncResponse>>

    @GET("api/sync/conflicts")
    suspend fun getConflicts(@Header("Authorization") token: String): Response<ApiResponse<List<SyncConflict>>>

    @POST("api/sync/conflicts/{id}/resolve")
    suspend fun resolveConflict(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: ConflictResolutionRequest
    ): Response<ApiResponse<Unit>>
}
