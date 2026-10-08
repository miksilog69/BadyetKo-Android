package com.badyetko.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

class AuthManager(private val activity: Activity, private val store: LocalStore) {
    fun signInGoogle() {
        val url = BuildConfig.SUPABASE_URL + "/auth/v1/authorize?provider=google&redirect_to=" +
            Uri.encode(BuildConfig.AUTH_REDIRECT)
        CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(url))
    }

    fun handleIntent(intent: Intent?): Boolean {
        val uri=intent?.data ?: return false
        if (uri.scheme!="badyetko" || uri.host!="login-callback") return false
        val fragment=uri.fragment.orEmpty()
        val map=fragment.split("&").mapNotNull { p -> p.split("=",limit=2).takeIf{it.size==2}?.let{Uri.decode(it[0]) to Uri.decode(it[1])} }.toMap()
        val access=map["access_token"].orEmpty(); val refresh=map["refresh_token"].orEmpty()
        if(access.isNotBlank()) { store.saveSession(access,refresh); return true }
        return false
    }

    fun signOut() = store.clearSession()
    fun signedIn(): Boolean = store.accessToken().isNotBlank()
}
