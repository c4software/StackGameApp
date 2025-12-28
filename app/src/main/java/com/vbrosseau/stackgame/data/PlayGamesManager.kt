package com.vbrosseau.stackgame.data

import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class PlayerInfo(
    val playerId: String,
    val displayName: String,
    val avatarUri: String? = null
)

class PlayGamesManager(private val activity: Activity) {
    
    private val _playerInfo = MutableStateFlow<PlayerInfo?>(null)
    val playerInfo: StateFlow<PlayerInfo?> = _playerInfo.asStateFlow()
    
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()
    
    private var googleSignInClient: GoogleSignInClient? = null
    
    companion object {
        private const val TAG = "PlayGamesManager"
    }
    
    fun initialize() {
        // Initialize Play Games SDK
        PlayGamesSdk.initialize(activity)
        
        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
        
        // Check if already signed in
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account != null) {
            handleSignInResult(account)
        }
    }
    
    suspend fun signIn(): Result<PlayerInfo> {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(activity)
            
            if (account != null) {
                handleSignInResult(account)
                Result.success(_playerInfo.value!!)
            } else {
                // Silent sign-in
                val silentSignInTask = googleSignInClient?.silentSignIn()
                if (silentSignInTask?.isSuccessful == true) {
                    val signInAccount = silentSignInTask.result
                    handleSignInResult(signInAccount)
                    Result.success(_playerInfo.value!!)
                } else {
                    // Need interactive sign-in
                    Result.failure(Exception("Interactive sign-in required"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            Result.failure(e)
        }
    }
    
    fun getSignInIntent() = googleSignInClient?.signInIntent
    
    fun handleSignInResult(account: GoogleSignInAccount) {
        try {
            val playerId = account.id ?: "unknown"
            val displayName = account.displayName ?: "Player"
            val avatarUri = account.photoUrl?.toString()
            
            _playerInfo.value = PlayerInfo(
                playerId = playerId,
                displayName = displayName,
                avatarUri = avatarUri
            )
            _isSignedIn.value = true
            
            Log.d(TAG, "Signed in as: $displayName (ID: $playerId)")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling sign-in result", e)
        }
    }
    
    suspend fun signOut() {
        try {
            googleSignInClient?.signOut()?.await()
            _playerInfo.value = null
            _isSignedIn.value = false
            Log.d(TAG, "Signed out")
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed", e)
        }
    }
}
