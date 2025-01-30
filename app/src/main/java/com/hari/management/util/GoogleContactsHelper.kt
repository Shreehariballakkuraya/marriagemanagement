package com.hari.management.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.people.v1.PeopleService
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

object GoogleContactsHelper {
    private lateinit var googleSignInClient: GoogleSignInClient
    private const val APPLICATION_NAME = "Wedding Management App"

    fun initGoogleSignIn(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/contacts.readonly"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun fetchContacts(context: Context): List<Contact> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) 
                ?: throw Exception("Not signed in")

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton("https://www.googleapis.com/auth/contacts.readonly")
            ).apply {
                selectedAccount = account.account
            }

            val peopleService = PeopleService.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(APPLICATION_NAME)
                .build()

            try {
                val response = peopleService.people().connections()
                    .list("people/me")
                    .setPersonFields("names,phoneNumbers")
                    .execute()

                response.connections?.mapNotNull { person ->
                    val name = person.names?.firstOrNull()?.displayName
                    val phone = person.phoneNumbers?.firstOrNull()?.value
                    if (name != null && phone != null) {
                        Contact(name, phone)
                    } else null
                } ?: emptyList()
            } catch (e: GoogleJsonResponseException) {
                when {
                    e.message?.contains("SERVICE_DISABLED") == true ->
                        throw Exception("People API is not enabled. Please enable it in Google Cloud Console.")
                    e.statusCode == 403 -> 
                        throw Exception("Permission denied. Please check OAuth configuration.")
                    else -> throw e
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleContactsHelper", "Error fetching contacts", e)
            throw e
        }
    }

    data class Contact(
        val name: String,
        val phoneNumber: String
    )
} 