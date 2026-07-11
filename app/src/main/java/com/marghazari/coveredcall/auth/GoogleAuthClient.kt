package com.marghazari.coveredcall.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.marghazari.coveredcall.R
import kotlinx.coroutines.tasks.await

class GoogleAuthClient(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val googleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun signInWithIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    fun currentUser() = auth.currentUser

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }
}
