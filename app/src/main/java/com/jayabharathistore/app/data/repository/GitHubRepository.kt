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
    private val GITHUB_OWNER = "yogesh5770"
    private val GITHUB_REPO = "jayabharathistore"
    private val GITHUB_TOKEN = "token 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"

    suspend fun triggerBuild(store: StoreProfile): Boolean {
        return try {
            val payload = mapOf(
                "store_id" to store.id,
                "store_name" to store.name,
                "icon_url" to store.userAppIconUrl
            )
            val response = api.triggerBuild(
                owner = GITHUB_OWNER, 
                repo = GITHUB_REPO, 
                token = GITHUB_TOKEN, 
                request = GitHubDispatchRequest(client_payload = payload)
            )
            
            if (!response.isSuccessful) {
                android.util.Log.e("GitHubAPI", "Failed: ${response.code()} - ${response.errorBody()?.string()}")
            }
            
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
