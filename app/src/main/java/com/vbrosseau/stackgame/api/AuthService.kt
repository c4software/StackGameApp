package com.vbrosseau.stackgame.api

import com.vbrosseau.stackgame.models.User
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

open class AuthService {
    private val client = ApiClient.client
    
    open suspend fun login(email: String, password: String): Result<User> {
        return try {
            // API fictive : GET https://cours.brosseau.ovh/api/EMAIL.json
            val response = client.get("${ApiClient.BASE_URL}/${email}.json")
            
            if (response.status == HttpStatusCode.OK) {
                val user: User = response.body()
                Result.success(user)
            } else {
                Result.failure(Exception("Utilisateur non trouvé"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de connexion: ${e.message}"))
        }
    }
}
