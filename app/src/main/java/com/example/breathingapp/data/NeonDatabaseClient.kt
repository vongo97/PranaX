package com.example.breathingapp.data

import com.example.breathingapp.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.*

object NeonDatabaseClient {
    private val client = HttpClient(OkHttp)

    private val connectionString: String by lazy {
        BuildConfig.NEON_URL
    }

    private val httpUrl: String by lazy {
        val rawUrl = connectionString
        val cleanUrl = rawUrl.removePrefix("postgresql://").removePrefix("postgres://")
        val atIndex = cleanUrl.indexOf('@')
        val hostDbParams = if (atIndex != -1) cleanUrl.substring(atIndex + 1) else cleanUrl
        val slashIndex = hostDbParams.indexOf('/')
        val host = if (slashIndex != -1) hostDbParams.substring(0, slashIndex) else hostDbParams
        "https://$host/sql"
    }

    suspend fun execute(query: String, params: List<Any?> = emptyList()): List<Map<String, JsonElement>> {
        val requestBody = buildJsonObject {
            put("query", query)
            put("params", buildJsonArray {
                params.forEach { param ->
                    when (param) {
                        is String -> add(param)
                        is Int -> add(param)
                        is Long -> add(param)
                        is Double -> add(param)
                        is Boolean -> add(param)
                        null -> add(JsonNull)
                        else -> add(param.toString())
                    }
                }
            })
        }

        val response = client.post(httpUrl) {
            contentType(ContentType.Application.Json)
            header("Neon-Connection-String", connectionString)
            setBody(requestBody.toString())
        }

        val responseText = response.bodyAsText()

        if (response.status.value !in 200..299) {
            val errorMsg = try {
                Json.parseToJsonElement(responseText).jsonObject["message"]?.jsonPrimitive?.content
            } catch (e: Exception) {
                null
            } ?: "Error de base de datos (${response.status.value}): $responseText"
            throw Exception(errorMsg)
        }

        val jsonResponse = try {
            Json.parseToJsonElement(responseText).jsonObject
        } catch (e: Exception) {
            throw Exception("Respuesta inválida de Neon: $responseText", e)
        }

        if (jsonResponse.containsKey("error")) {
            val errorMsg = jsonResponse["error"]?.jsonPrimitive?.content ?: "Error en la consulta Neon SQL"
            throw Exception(errorMsg)
        }

        val rowsElement = jsonResponse["rows"]?.jsonArray
        val resultList = mutableListOf<Map<String, JsonElement>>()
        if (rowsElement != null) {
            for (row in rowsElement) {
                resultList.add(row.jsonObject.toMap())
            }
        }
        return resultList
    }
}
