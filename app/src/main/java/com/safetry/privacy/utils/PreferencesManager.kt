package com.safetry.privacy.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_settings")

class PreferencesManager(private val context: Context) {
    companion object {
        val BLUR_FACES = booleanPreferencesKey("blur_faces")
        val BLUR_LICENSE_PLATES = booleanPreferencesKey("blur_license_plates")
        val BLUR_STREET_SIGNS = booleanPreferencesKey("blur_street_signs")
        val BLUR_ID_BADGES = booleanPreferencesKey("blur_id_badges")
        val BLUR_TEXT_DOCS = booleanPreferencesKey("blur_text_docs")
        val AUTO_REMOVE_METADATA = booleanPreferencesKey("auto_remove_metadata")
    }

    fun getBlurFaces(): Flow<Boolean> = context.dataStore.data.map { it[BLUR_FACES] ?: false }
    suspend fun setBlurFaces(v: Boolean) = context.dataStore.edit { it[BLUR_FACES] = v }

    fun getBlurLicensePlates(): Flow<Boolean> = context.dataStore.data.map { it[BLUR_LICENSE_PLATES] ?: true }
    suspend fun setBlurLicensePlates(v: Boolean) = context.dataStore.edit { it[BLUR_LICENSE_PLATES] = v }

    fun getBlurStreetSigns(): Flow<Boolean> = context.dataStore.data.map { it[BLUR_STREET_SIGNS] ?: true }
    suspend fun setBlurStreetSigns(v: Boolean) = context.dataStore.edit { it[BLUR_STREET_SIGNS] = v }

    fun getBlurIdBadges(): Flow<Boolean> = context.dataStore.data.map { it[BLUR_ID_BADGES] ?: true }
    suspend fun setBlurIdBadges(v: Boolean) = context.dataStore.edit { it[BLUR_ID_BADGES] = v }

    fun getBlurTextDocs(): Flow<Boolean> = context.dataStore.data.map { it[BLUR_TEXT_DOCS] ?: true }
    suspend fun setBlurTextDocs(v: Boolean) = context.dataStore.edit { it[BLUR_TEXT_DOCS] = v }

    fun getAutoRemoveMetadata(): Flow<Boolean> = context.dataStore.data.map { it[AUTO_REMOVE_METADATA] ?: true }
    suspend fun setAutoRemoveMetadata(v: Boolean) = context.dataStore.edit { it[AUTO_REMOVE_METADATA] = v }
}
