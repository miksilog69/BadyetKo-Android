package com.badyetko.app

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ApiClient(private val store: LocalStore) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val media = "application/json; charset=utf-8".toMediaType()

    private fun request(path: String, method: String = "GET", body: String? = null): String {
        val b = Request.Builder().url(BuildConfig.API_BASE + path)
            .header("Content-Type", "application/json")
        val token = store.accessToken()
        if (token.isNotBlank()) b.header("Authorization", "Bearer $token")
        when (method) {
            "POST" -> b.post((body ?: "{}").toRequestBody(media))
            "DELETE" -> b.delete((body ?: "").toRequestBody(media))
            else -> b.get()
        }
        client.newCall(b.build()).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException(runCatching { Json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull() ?: "Request failed (${r.code})")
            return text
        }
    }

    fun loadBudget(ownerId: String? = null): BudgetState {
        val query = ownerId?.let { "?ownerId=${java.net.URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
        val root = Json.parseToJsonElement(request("/api/budget$query")).jsonObject
        val data = root["data"] ?: return BudgetState()
        return json.decodeFromJsonElement(BudgetState.serializer(), data)
    }

    fun saveBudget(state: BudgetState, ownerId: String? = null) {
        val payload = buildJsonObject {
            put("data", json.encodeToJsonElement(BudgetState.serializer(), state))
            ownerId?.let { put("ownerId", it) } ?: put("ownerId", JsonNull)
        }
        val query = ownerId?.let { "?ownerId=${java.net.URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
        request("/api/budget$query", "POST", payload.toString())
    }

    fun collabDashboard(): JsonObject = Json.parseToJsonElement(request("/api/collab?action=dashboard")).jsonObject
    fun challenges(): JsonObject = Json.parseToJsonElement(request("/api/challenge")).jsonObject
    fun activity(): JsonObject = Json.parseToJsonElement(request("/api/activity")).jsonObject

    fun sharedBudgets(): List<SharedBudget> {
        val arr = collabDashboard()["shared"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { e ->
            val o=e.jsonObject
            val owner=o["owner_user_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            SharedBudget(
                id=o["id"]?.jsonPrimitive?.contentOrNull.orEmpty(), ownerUserId=owner,
                ownerName=o["owner_name"]?.jsonPrimitive?.contentOrNull ?: "Shared budget",
                ownerEmail=o["owner_email"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                permission=o["permission"]?.jsonPrimitive?.contentOrNull ?: "view",
                shareMonth=o["share_month"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
        }
    }

    fun refreshSession(): Boolean {
        val refresh = store.refreshToken(); if (refresh.isBlank()) return false
        val form = FormBody.Builder().add("refresh_token", refresh).build()
        val req = Request.Builder()
            .url(BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .post(form).build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return false
            val o = Json.parseToJsonElement(r.body?.string().orEmpty()).jsonObject
            val access=o["access_token"]?.jsonPrimitive?.contentOrNull ?: return false
            val next=o["refresh_token"]?.jsonPrimitive?.contentOrNull ?: refresh
            store.saveSession(access,next); return true
        }
    }
}
