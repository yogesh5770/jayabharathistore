package com.jayabharathistore.app.data.repository

import com.jayabharathistore.app.data.api.GitHubApi
import com.jayabharathistore.app.data.api.GitHubDispatchRequest
import com.jayabharathistore.app.data.model.StoreProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val api: GitHubApi
) {
    // Configuration - Move these to build secrets or remote config in production
    private val GITHUB_OWNER = "YOUR_GITHUB_USERNAME"
    private val GITHUB_REPO = "YOUR_REPO_NAME"
    private val GITHUB_TOKEN = "Bearer YOUR_PERSONAL_ACCESS_TOKEN"

    suspend fun triggerBuild(store: StoreProfile): Boolean {
        return try {
            val payload = mapOf(
                "store_id" to store.id,
                "store_name" to store.name,
                "icon_url" to store.userAppIconUrl
            )
            val response = api.triggerBuild(
                GITHUB_OWNER, 
                GITHUB_REPO, 
                GITHUB_TOKEN, 
                GitHubDispatchRequest(client_payload = payload)
            )
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
