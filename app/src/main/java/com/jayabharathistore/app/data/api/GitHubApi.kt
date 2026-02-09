package com.jayabharathistore.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GitHubApi {
    @POST("repos/{owner}/{repo}/dispatches")
    suspend fun triggerBuild(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/vnd.github.v3+json",
        @Body request: GitHubDispatchRequest
    ): Response<Unit>
}

data class GitHubDispatchRequest(
    val event_type: String = "build-white-label",
    val client_payload: Map<String, String>
)
