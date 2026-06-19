package com.example.arplitka.shared.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File

fun createAndroidDataStore(context: Context): DataStore<Preferences> {
    val appContext = context.applicationContext
    return PreferenceDataStoreFactory.create(
        produceFile = {
            File(appContext.filesDir, AppConfigManager.DATASTORE_FILE_NAME)
        }
    )
}
