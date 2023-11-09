package ru.telecor.gm.mobile.droid.entities

import com.google.gson.annotations.SerializedName

enum class StatusType {
    @SerializedName("NEW")
    NEW,
    @SerializedName("SUCCESS")
    SUCCESS,
    @SerializedName("PARTLY")
    PARTIALLY,
    @SerializedName("FAIL")
    FAIL
}


