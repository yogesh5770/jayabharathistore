package com.jayabharathistore.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Uploads an image to Catbox.moe (Free anonymous hosting) and returns a clean HTTPS URL.
     * This avoids Firebase Storage and Base64 size limits.
     */
    suspend fun uploadProductImage(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val boundary = "Boundary-${System.currentTimeMillis()}"
        val lineEnd = "\r\n"
        val twoHyphens = "--"
        
        try {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Uploading image to cloud...", android.widget.Toast.LENGTH_SHORT).show()
            }

            // 1. Process and compress image locally first to save data
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: throw Exception("File error")
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            val scaled = if (original.width > 1024) {
                val ratio = 1024f / original.width
                Bitmap.createScaledBitmap(original, 1024, (original.height * ratio).toInt(), true)
            } else original
            
            val bos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, bos)
            val imageData = bos.toByteArray()
            
            // 2. Upload to Catbox
            val url = URL("https://catbox.moe/user/api.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                doInput = true
                doOutput = true
                useCaches = false
                requestMethod = "POST"
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")
            }

            DataOutputStream(conn.outputStream).use { dos ->
                // Add reqtype field
                dos.writeBytes(twoHyphens + boundary + lineEnd)
                dos.writeBytes("Content-Disposition: form-data; name=\"reqtype\"$lineEnd$lineEnd")
                dos.writeBytes("fileupload$lineEnd")

                // Add file field
                dos.writeBytes(twoHyphens + boundary + lineEnd)
                dos.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"product.jpg\"$lineEnd")
                dos.writeBytes("Content-Type: image/jpeg$lineEnd$lineEnd")
                dos.write(imageData)
                dos.writeBytes(lineEnd)

                // End of multipart
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
                dos.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val result = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                if (result.startsWith("https://")) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Image Uploaded Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    result
                } else {
                    throw Exception("Cloud Response: $result")
                }
            } else {
                throw Exception("Server Error Code: $responseCode")
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Cloud Upload Failed", e)
            throw Exception("Cloud Image Failed: ${e.localizedMessage}")
        }
    }

    suspend fun deleteProductImage(imageUrl: String) {
        // Catbox doesn't support easy anonymous deletion via API
    }
}
