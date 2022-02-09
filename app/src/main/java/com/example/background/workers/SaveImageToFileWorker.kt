package com.example.background.workers

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns.IS_PENDING
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SaveImageToFileWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z",
        Locale.getDefault()
    )

    override fun doWork(): Result {
        val relativeLocation = "$DIRECTORY_PICTURES/Bitmaps"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, title)
            put(MediaStore.MediaColumns.DATE_ADDED, dateFormatter.toString())
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
                put(IS_PENDING, 1)
            }
        }

        val resolver = applicationContext.contentResolver
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        val bitmap = BitmapFactory.decodeStream(
            resolver.openInputStream(Uri.parse(resourceUri))
        )

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        makeStatusNotification("Saving Image", applicationContext)
        sleep()

        return try {

            uri?.let { uri ->
                val stream = resolver.openOutputStream(uri)

                stream?.let { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)) {
                        throw IOException("Failed to save bitmap.")
                    }
                    contentValues.clear()
                    contentValues.put(IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                } ?: throw IOException("Failed to get output stream.")

            } ?: throw IOException("Failed to create new MediaStore record")

            val output = workDataOf(KEY_IMAGE_URI to uri.toString())

            makeStatusNotification("Image Saved", applicationContext)
            Result.success(output)
        } catch (e: IOException) {
            if (uri != null) {
                resolver.delete(uri, null, null)
            }
            throw IOException(e)
            Result.failure()
        }
    }
}