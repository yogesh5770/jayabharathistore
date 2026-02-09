package com.jayabharathistore.app.data.util

import android.content.Context
import com.jayabharathistore.app.R

class StoreConfig(private val context: Context) {
    /**
     * returns the Store ID if this is a white-labeled build.
     * The ID is injected by the build script into res/values/config.xml
     */
    fun getTargetStoreId(): String? {
        return try {
            val id = context.getString(R.string.target_store_id)
            if (id.isBlank()) null else id
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns true if the app is a white-labeled build for a specific store.
     */
    fun isWhiteLabeled(): Boolean = getTargetStoreId() != null
}
