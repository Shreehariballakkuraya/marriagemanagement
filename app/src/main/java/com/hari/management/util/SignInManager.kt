package com.hari.management.util

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SignInManager {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val _isSignedIn = MutableStateFlow<Boolean?>(null)
    val isSignedIn: StateFlow<Boolean?> = _isSignedIn

    fun init(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/contacts.readonly"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
        checkSignInStatus(context)
    }

    fun getSignInIntent() = googleSignInClient.signInIntent

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            _isSignedIn.value = false
            onComplete()
        }
    }

    fun checkSignInStatus(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        _isSignedIn.value = account != null && !account.isExpired
    }

    fun handleSignInResult(account: GoogleSignInAccount?) {
        _isSignedIn.value = account != null
    }
} 