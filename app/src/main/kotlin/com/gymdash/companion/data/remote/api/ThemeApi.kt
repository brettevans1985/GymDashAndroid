package com.gymdash.companion.data.remote.api

import com.gymdash.companion.data.remote.dto.ThemeListResponseDto
import com.gymdash.companion.data.remote.dto.ThemePreferenceDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

interface ThemeApi {

    @GET("themes")
    suspend fun getThemes(
        @Header("Authorization") token: String
    ): ThemeListResponseDto

    @PUT("themes/preference")
    suspend fun setPreference(
        @Header("Authorization") token: String,
        @Body preference: ThemePreferenceDto
    ): ThemePreferenceDto
}
