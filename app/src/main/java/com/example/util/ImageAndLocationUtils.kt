package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class CapturedLocation(
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double
)

fun createTempCameraUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val photoFile = File.createTempFile("cleancity_", ".jpg", imagesDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}

suspend fun compressUriToBase64(context: Context, uri: Uri): Result<String> =
    withContext(Dispatchers.IO) {
        try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Unable to decode selected image."))
            }

            val maxDim = 480
            var sampleSize = 1
            while ((origW / sampleSize) > maxDim * 2 || (origH / sampleSize) > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext Result.failure(IllegalArgumentException("Could not open image stream."))

            val scale = minOf(1f, maxDim.toFloat() / max(decoded.width, decoded.height))
            val targetBitmap = if (scale < 1f) {
                val w = max(1, (decoded.width * scale).toInt())
                val h = max(1, (decoded.height * scale).toInt())
                Bitmap.createScaledBitmap(decoded, w, h, true).also {
                    if (it != decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            var quality = 75
            var jpegBytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                targetBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                jpegBytes = out.toByteArray()
                quality -= 15
            } while (jpegBytes.size > 180_000 && quality >= 25)

            targetBitmap.recycle()

            val encoded = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            if (encoded.length > 340_000) {
                Result.failure(IllegalArgumentException("Image is too large even after compression."))
            } else {
                Result.success(encoded)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

fun decodeBase64ToBitmap(base64: String): Bitmap? {
    if (base64.isBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

@SuppressLint("MissingPermission")
suspend fun captureCurrentLocation(context: Context): Result<CapturedLocation> =
    withContext(Dispatchers.IO) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                ?: client.lastLocation.await()
                ?: return@withContext Result.failure(
                    IllegalStateException("GPS signal unavailable. Please enable location or enter address manually.")
                )

            val lat = location.latitude
            val lon = location.longitude
            val coordLabel = String.format(Locale.US, "%.5f, %.5f", lat, lon)

            val addressString = try {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
                val first = addresses?.firstOrNull()
                if (first != null) {
                    val parts = listOfNotNull(
                        first.thoroughfare ?: first.featureName,
                        first.subLocality ?: first.locality
                    ).distinct().filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) "${parts.joinToString(", ")} ($coordLabel)" else "GPS: $coordLabel"
                } else {
                    "GPS: $coordLabel"
                }
            } catch (_: Exception) {
                "GPS: $coordLabel"
            }

            Result.success(
                CapturedLocation(
                    formattedAddress = addressString.take(195),
                    latitude = lat,
                    longitude = lon
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

fun formatTimestamp(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return "Just now"
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
}

fun formatMillis(millis: Long): String {
    if (millis <= 0L) return "Just now"
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
}
