package com.medgaus.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface YouTubeWebApi {

    companion object {
        const val SERVER_BASE_URL = "https://www.youtube.com/"
    }

    @GET("results")
    suspend fun search(@Query("search_query") query: String): Response<ResponseBody>

}
