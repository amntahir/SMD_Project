package com.example.smd_project

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Central Retrofit API interface.
 *
 * NOTE: All model classes (SignupRequest, SimpleResponse, LoginRequest, etc.)
 * must exist in package `com.example.smd_project`.
 */
interface ApiService {

    @POST("signup.php")
    suspend fun signup(@Body req: SignupRequest): Response<SimpleResponse>

    @POST("login.php")
    suspend fun login(@Body req: LoginRequest): Response<SimpleResponse>

    @POST("forgot_password.php")
    suspend fun forgotPassword(@Body req: ForgotRequest): Response<SimpleResponse>

    @POST("reset_password.php")
    suspend fun resetPassword(@Body req: ResetRequest): Response<SimpleResponse>

    @Multipart
    @POST("upload.php")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("user_id") userId: RequestBody,
        @Part("originalName") originalName: RequestBody
    ): Response<UploadResponse>

    @POST("get_docs.php")
    suspend fun getDocs(@Body req: Map<String, String>): Response<DocumentsResponse>

    // Save eligibility: server returns SaveEligibilityResponse wrapped in Response
    @POST("api/save_eligibility.php")
    suspend fun saveEligibility(@Body req: EligibilityRequest): Response<SaveEligibilityResponse>

    // Get eligibility wrapper — wrapped in Response for consistent HTTP handling
    @GET("api/get_eligibility.php")
    suspend fun getEligibility(@Query("userId") userId: String): Response<GetEligibilityResponseWrapper>
}
