package com.lab.assistant.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ModelDownloadWorker(ctx: Context, params: androidx.work.WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("file") ?: return Result.failure()
        val out = File(File(applicationContext.filesDir, "models"), fileName)
        out.parentFile?.mkdirs()
        return try {
            val tmp = File(out.path + ".part")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
            }
            conn.connect()
            val total = conn.contentLengthLong.coerceAtLeast(1)
            conn.inputStream.use { ins ->
                FileOutputStream(tmp).use { ous ->
                    val buf = ByteArray(256 * 1024)
                    var done = 0L
                    while (true) {
                        val n = ins.read(buf)
                        if (n <= 0) break
                        ous.write(buf, 0, n)
                        done += n
                        setProgress(workDataOf("progress" to (done * 100 / total).toInt()))
                    }
                }
            }
            tmp.renameTo(out)
            Result.success(Data.Builder().putString("path", out.absolutePath).build())
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object ModelDownloader {
    const val PUBLIC_SUBDIR = "LabAssistant"

    fun publicDir(): File {
        val d = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            PUBLIC_SUBDIR
        )
        d.mkdirs()
        return d
    }

    fun legacyDir(ctx: Context) = File(ctx.filesDir, "models").apply { mkdirs() }

    fun fileFor(ctx: Context, m: OpenModel): File {
        val pub = File(publicDir(), m.fileName)
        if (pub.exists()) return pub
        val leg = File(legacyDir(ctx), m.fileName)
        if (leg.exists()) return leg
        return pub
    }

    fun isDownloaded(ctx: Context, m: OpenModel) = fileFor(ctx, m).exists()

    fun enqueue(ctx: Context, m: OpenModel): Long {
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(m.url))
            .setTitle(m.name)
            .setDescription("Lab Assistant model")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$PUBLIC_SUBDIR/${m.fileName}")
        return dm.enqueue(req)
    }

    fun info(ctx: Context, id: UUID): androidx.lifecycle.LiveData<WorkInfo?> =
        WorkManager.getInstance(ctx).getWorkInfoByIdLiveData(id)

    fun delete(ctx: Context, m: OpenModel) {
        File(publicDir(), m.fileName).delete()
        File(legacyDir(ctx), m.fileName).delete()
        try {
            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val q = DownloadManager.Query()
            val c = dm.query(q)
            c?.use {
                while (it.moveToNext()) {
                    val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    if (uri != null && uri.endsWith(m.fileName)) {
                        val did = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                        dm.remove(did)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
