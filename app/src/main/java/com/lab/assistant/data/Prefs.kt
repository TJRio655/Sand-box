package com.lab.assistant.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.store by preferencesDataStore("lab_prefs")

object Prefs {
    val MODEL_PATH = stringPreferencesKey("model_path")
    val GPU = booleanPreferencesKey("gpu")
    val TEMP = floatPreferencesKey("temp")
    val MAX_TOK = intPreferencesKey("max_tok")

    fun modelPath(ctx: Context) = ctx.store.data.map { it[MODEL_PATH] ?: "" }
    fun gpu(ctx: Context) = ctx.store.data.map { it[GPU] ?: true }
    fun temp(ctx: Context) = ctx.store.data.map { it[TEMP] ?: 0.8f }
    fun maxTok(ctx: Context) = ctx.store.data.map { it[MAX_TOK] ?: 1024 }

    suspend fun setModelPath(ctx: Context, v: String) { ctx.store.edit { it[MODEL_PATH] = v } }
    suspend fun setGpu(ctx: Context, v: Boolean) { ctx.store.edit { it[GPU] = v } }
    suspend fun setTemp(ctx: Context, v: Float) { ctx.store.edit { it[TEMP] = v } }
    suspend fun setMaxTok(ctx: Context, v: Int) { ctx.store.edit { it[MAX_TOK] = v } }
}
