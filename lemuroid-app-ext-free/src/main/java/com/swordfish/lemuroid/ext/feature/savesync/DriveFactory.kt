package com.swordfish.lemuroid.ext.feature.savesync

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

@SuppressLint("RestrictedApi")
class DriveFactory(private val context: Context) {
    fun create(): Drive? {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context) ?: return null

        val scopeNames = listOf(Scope(DriveScopes.DRIVE_APPDATA).toString())

        val credential = GoogleAccountCredential.usingOAuth2(context, scopeNames)
        credential.selectedAccount = googleAccount.account

        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        return Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("com.swordfish.lemuroid")
            .build()
    }
}
