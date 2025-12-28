package com.vbrosseau.stackgame.api

import com.vbrosseau.stackgame.models.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiClient {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        
        install(Logging) {
            level = LogLevel.ALL
        }
    }
    
    companion object {
        private const val BASE_URL = "https://cours.brosseau.ovh/api"
    }
    
    suspend fun login(email: String): Result<User> {
        return try {
            val url = "$BASE_URL/$email.json"
            val response = client.get(url)
            
            if (response.status.value == 404) {
               return Result.failure(Exception("Utilisateur non trouvé"))
            } else if (response.status.value !in 200..299) {
               return Result.failure(Exception("Erreur serveur: ${response.status.value}"))
            }
            
            val user: User = response.body()
            Result.success(user)
        } catch (e: Exception) {
            // Network or parsing errors
            Result.failure(e)
        }
    }
}
