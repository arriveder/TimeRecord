package com.example.timerecord.network.dto

import com.google.gson.annotations.SerializedName

data class RecordRequest(
    @SerializedName("note") val note: String?,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("date") val date: String,
    @SerializedName("time24") val time24: String,
    @SerializedName("time12") val time12: String,
    @SerializedName("amPm") val amPm: String,
    @SerializedName("labelIds") val labelIds: List<String>? = null
)

data class RecordResponse(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("note") val note: String?,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("date") val date: String,
    @SerializedName("time24") val time24: String,
    @SerializedName("time12") val time12: String,
    @SerializedName("amPm") val amPm: String,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("labels") val labels: List<LabelResponse>?
)

data class LabelResponse(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class LabelRequest(
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String?
)
