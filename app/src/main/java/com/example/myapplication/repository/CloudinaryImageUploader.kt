package com.example.myapplication.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class CloudinaryUploadResult(
    val secureUrl: String,
    val publicId: String
)

object CloudinaryImageUploader {

    // Configurable backend signing endpoint URL
    var signingServerUrl: String = "https://nesta-cloudinary-signer.workers.dev"

    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        propertyId: String,
        onProgress: (Float) -> Unit = {}
    ): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: return@withContext Result.failure(Exception("Authentication required to upload listing photos."))

            // Fetch fresh Firebase ID token
            val tokenResult = user.getIdToken(false).await()
            val idToken = tokenResult.token ?: return@withContext Result.failure(Exception("Failed to acquire Firebase authentication token."))

            // Step 1: Request signed upload authorization from backend signing server
            onProgress(0.1f)
            val signUrl = URL(signingServerUrl)
            val signConn = (signUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $idToken")
            }

            val signBody = JSONObject().apply {
                put("propertyId", propertyId)
                put("ownerId", user.uid)
            }.toString()

            OutputStreamWriter(signConn.outputStream).use { writer ->
                writer.write(signBody)
            }

            if (signConn.responseCode != 200) {
                val errorMsg = signConn.errorStream?.bufferedReader()?.readText() ?: "Signing server rejected request (${signConn.responseCode})."
                val jsonErr = try { JSONObject(errorMsg).optString("error", errorMsg) } catch (_: Exception) { errorMsg }
                return@withContext Result.failure(Exception("Image upload unauthorized: $jsonErr"))
            }

            val signRespText = signConn.inputStream.bufferedReader().readText()
            val signJson = JSONObject(signRespText)

            val signature = signJson.getString("signature")
            val timestamp = signJson.getLong("timestamp")
            val folder = signJson.getString("folder")
            val publicId = signJson.getString("publicId")
            val overwrite = signJson.getBoolean("overwrite")
            val apiKey = signJson.getString("apiKey")
            val cloudName = signJson.getString("cloudName")

            onProgress(0.3f)

            // Step 2: Multipart form POST directly to Cloudinary upload endpoint
            val boundary = "---NestaBoundary${UUID.randomUUID()}"
            val cloudinaryUrl = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            val conn = (cloudinaryUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Failed to read image file from device storage."))

            val outputStream = conn.outputStream
            val writer = OutputStreamWriter(outputStream, "UTF-8")

            fun addFormField(name: String, value: String) {
                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                writer.append("$value\r\n")
                writer.flush()
            }

            // Submit exact parameters covered by signature in official Cloudinary format
            addFormField("api_key", apiKey)
            addFormField("folder", folder)
            addFormField("overwrite", overwrite.toString())
            addFormField("public_id", publicId)
            addFormField("signature", signature)
            addFormField("timestamp", timestamp.toString())

            // Add File Binary
            writer.append("--$boundary\r\n")
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"upload.jpg\"\r\n")
            writer.append("Content-Type: image/jpeg\r\n\r\n")
            writer.flush()

            val buffer = ByteArray(4096)
            var bytesRead: Int
            var totalRead = 0L
            val fileSize = inputStream.available().coerceAtLeast(1)

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val progress = 0.3f + (0.6f * (totalRead.toFloat() / fileSize))
                onProgress(progress.coerceAtMost(0.9f))
            }

            outputStream.flush()
            writer.append("\r\n--$boundary--\r\n")
            writer.flush()
            inputStream.close()

            onProgress(0.95f)

            if (conn.responseCode != 200) {
                val errText = conn.errorStream?.bufferedReader()?.readText() ?: "Cloudinary upload failed with code ${conn.responseCode}"
                return@withContext Result.failure(Exception("Cloudinary upload failed: $errText"))
            }

            val respText = conn.inputStream.bufferedReader().readText()
            val respJson = JSONObject(respText)

            val secureUrl = respJson.getString("secure_url")
            val returnedPublicId = respJson.getString("public_id")

            onProgress(1.0f)
            Result.success(CloudinaryUploadResult(secureUrl, returnedPublicId))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Image upload encountered an unexpected error.", e))
        }
    }

    suspend fun deleteImage(propertyId: String, publicId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: return@withContext Result.failure(Exception("Authentication required."))

            val tokenResult = user.getIdToken(false).await()
            val idToken = tokenResult.token ?: return@withContext Result.failure(Exception("Failed to acquire token."))

            val signUrl = URL(signingServerUrl)
            val signConn = (signUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $idToken")
            }

            val body = JSONObject().apply {
                put("propertyId", propertyId)
                put("publicId", publicId)
                put("action", "destroy")
            }.toString()

            OutputStreamWriter(signConn.outputStream).use { it.write(body) }

            if (signConn.responseCode != 200) {
                val errorMsg = signConn.errorStream?.bufferedReader()?.readText() ?: "Delete request rejected."
                return@withContext Result.failure(Exception("Delete unauthorized: $errorMsg"))
            }

            val signRespText = signConn.inputStream.bufferedReader().readText()
            val signJson = JSONObject(signRespText)

            val signature = signJson.getString("signature")
            val timestamp = signJson.getLong("timestamp")
            val apiKey = signJson.getString("apiKey")
            val cloudName = signJson.getString("cloudName")

            val destroyUrl = URL("https://api.cloudinary.com/v1_1/$cloudName/image/destroy")
            val conn = (destroyUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            val postParams = "public_id=${publicId}&timestamp=${timestamp}&api_key=${apiKey}&signature=${signature}"
            OutputStreamWriter(conn.outputStream).use { it.write(postParams) }

            if (conn.responseCode == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Cloudinary destroy failed (${conn.responseCode})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Delete image failed.", e))
        }
    }
}
