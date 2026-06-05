package moe.chenxy.oppopods.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import moe.chenxy.oppopods.utils.miuiStrongToast.data.OppoPodsPrefsKey

class OppoPodsPrefsProvider : ContentProvider() {

    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != METHOD_GET_BOOLEAN) return null

        val key = arg ?: extras?.getString(EXTRA_KEY) ?: return null
        if (key !in READABLE_BOOLEAN_KEYS) return null

        val defaultValue = extras?.getBoolean(EXTRA_DEFAULT, false) ?: false
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs?.getBoolean(key, defaultValue) ?: defaultValue

        return Bundle().apply {
            putBoolean(EXTRA_VALUE, value)
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun getType(uri: Uri): String? = null

    companion object {
        const val AUTHORITY = "moe.chenxy.oppopods.prefs"
        const val PREFS_NAME = "oppopods_settings"
        const val METHOD_GET_BOOLEAN = "getBoolean"
        const val EXTRA_KEY = "key"
        const val EXTRA_DEFAULT = "default"
        const val EXTRA_VALUE = "value"

        private val READABLE_BOOLEAN_KEYS = setOf(
            "auto_game_mode",
            "adaptive_mode",
            OppoPodsPrefsKey.PERSISTENT_ISLAND
        )

        fun readBoolean(
            context: Context,
            key: String,
            defaultValue: Boolean,
            onError: ((Exception) -> Unit)? = null
        ): Boolean {
            return try {
                context.contentResolver.call(
                    Uri.parse("content://$AUTHORITY"),
                    METHOD_GET_BOOLEAN,
                    key,
                    Bundle().apply { putBoolean(EXTRA_DEFAULT, defaultValue) }
                )?.getBoolean(EXTRA_VALUE, defaultValue) ?: defaultValue
            } catch (e: Exception) {
                onError?.invoke(e)
                defaultValue
            }
        }
    }
}
