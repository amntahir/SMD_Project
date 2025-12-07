package com.example.smd_project

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

// ---------- Retrofit interfaces ----------
interface EligibilityApiService {

    // Get eligibility by user id (returns wrapper)
    @GET("api/get_eligibility.php")
    suspend fun getEligibility(@Query("userId") userId: String): GetEligibilityResponseWrapper

    // Optional node endpoint kept for compatibility
    @GET("api/eligibility/get_for_user")
    suspend fun getEligibilityForUser(@Query("userId") userId: String): Response<EligibilityResponse>

    // Save eligibility (server returns Response<EligibilityResponse>)
    @POST("api/eligibility/save")
    suspend fun saveEligibility(@Body req: EligibilityRequest): Response<EligibilityResponse>

    // Upload supporting document: user_id as RequestBody, file as MultipartBody.Part
    @Multipart
    @POST("api/eligibility/upload_supporting")
    suspend fun uploadSupportingDocument(
        @Part("user_id") userId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<SaveEligibilityResponse>
}

// ---------- RetrofitProvider ----------
object RetrofitProvider {

    // Update base URL if required
    const val BASE_URL = "http://192.168.100.156/myapp/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    fun createAuthService(): ApiService = retrofit.create(ApiService::class.java)
    fun createEligibilityService(): EligibilityApiService = retrofit.create(EligibilityApiService::class.java)
    fun create(): ApiService = createAuthService()

    // safe wrappers (return Result)
    suspend fun saveEligibilitySafe(req: EligibilityRequest): Result<Response<EligibilityResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val api = createEligibilityService()
                val resp = api.saveEligibility(req)
                Result.success(resp)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getEligibilitySafe(userId: String): Result<GetEligibilityResponseWrapper> {
        return withContext(Dispatchers.IO) {
            try {
                val api = createEligibilityService()
                val resp = api.getEligibility(userId)
                Result.success(resp)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun uploadSupportingDocumentSafe(userIdReqBody: RequestBody, filePart: MultipartBody.Part): Result<SaveEligibilityResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val api = createEligibilityService()
                val resp = api.uploadSupportingDocument(userIdReqBody, filePart)
                if (resp.isSuccessful) {
                    resp.body()?.let { Result.success(it) } ?: Result.failure(NullPointerException("Response body null"))
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
