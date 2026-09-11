package com.lab.assistant.data

import android.content.Context
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
    fun modelsDir(ctx: Context) = File(ctx.filesDir, "models").apply { mkdirs() }
    fun fileFor(ctx: Context, m: OpenModel) = File(modelsDir(ctx), m.fileName)
    fun isDownloaded(ctx: Context, m: OpenModel) = fileFor(ctx, m).exists()

    fun enqueue(ctx: Context, m: OpenModel): UUID {
        val req = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf("url" to m.url, "file" to m.fileName))
            .build()
        WorkManager.getInstance(ctx).enqueue(req)
        return req.id
    }
    fun info(ctx: Context, id: UUID): androidx.lifecycle.LiveData<WorkInfo?> =
        WorkManager.getInstance(ctx).getWorkInfoByIdLiveData(id)

    fun delete(ctx: Context, m: OpenModel) { fileFor(ctx, m).delete() }
}
