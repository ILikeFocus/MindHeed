package com.mindheed.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object Instagram {

    const val PACKAGE = "com.instagram.android"

    fun normalizeUsername(raw: String): String =
        raw.trim().removePrefix("@").trim().trimEnd('/')

    /**
     * Opens the saved-posts page, in the Instagram app when installed, otherwise in the browser.
     * Returns false if nothing could open it.
     */
    fun openSavedPosts(context: Context, username: String): Boolean {
        val uri = Uri.parse("https://www.instagram.com/${Uri.encode(username)}/saved/")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(Intent(intent).setPackage(PACKAGE))
            true
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(intent)
                true
            } catch (e: ActivityNotFoundException) {
                false
            }
        }
    }
}
