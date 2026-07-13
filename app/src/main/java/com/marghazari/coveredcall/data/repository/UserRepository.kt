package com.marghazari.coveredcall.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.marghazari.coveredcall.data.model.AppUser
import com.marghazari.coveredcall.data.model.FeedbackMessage
import com.marghazari.coveredcall.data.model.ReceiptStatus
import com.marghazari.coveredcall.data.model.ReceiptSubmission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    suspend fun ensureUserAccountExists(uid: String, email: String, displayName: String) {
        val doc = userDoc(uid).get().await()
        if (!doc.exists()) {
            val newUser = AppUser(uid = uid, email = email, displayName = displayName)
            userDoc(uid).set(newUser).await()
        }
    }

    fun observeUser(uid: String): Flow<AppUser?> = callbackFlow {
        val registration = userDoc(uid).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.toObject(AppUser::class.java))
        }
        awaitClose { registration.remove() }
    }

    suspend fun submitReceipt(uid: String, imageUri: Uri, amountToman: Long = 100_000): String {
        val receiptId = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("receipts/$uid/$receiptId.jpg")
        storageRef.putFile(imageUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        val submission = ReceiptSubmission(
            id = receiptId,
            uid = uid,
            imageUrl = downloadUrl,
            submittedAtMillis = System.currentTimeMillis(),
            amountToman = amountToman,
            status = ReceiptStatus.PENDING
        )
        userDoc(uid).collection("receipts").document(receiptId).set(submission).await()
        return receiptId
    }

    fun observeReceipts(uid: String): Flow<List<ReceiptSubmission>> = callbackFlow {
        val registration = userDoc(uid).collection("receipts")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(ReceiptSubmission::class.java) } ?: emptyList()
                trySend(list.sortedByDescending { it.submittedAtMillis })
            }
        awaitClose { registration.remove() }
    }

    suspend fun activateSubscription(uid: String, durationMillis: Long = 30L * 24 * 60 * 60 * 1000) {
        val expiry = System.currentTimeMillis() + durationMillis
        userDoc(uid).update(
            mapOf(
                "isSubscribed" to true,
                "subscriptionExpiryMillis" to expiry
            )
        ).await()
    }

    // --------------------------------------------------------------------------------------------
    // سوالات و انتقادات
    // --------------------------------------------------------------------------------------------

    suspend fun submitFeedback(uid: String, email: String, message: String): String {
        val id = UUID.randomUUID().toString()
        val feedback = FeedbackMessage(
            id = id,
            uid = uid,
            email = email,
            message = message,
            submittedAtMillis = System.currentTimeMillis()
        )
        userDoc(uid).collection("feedback").document(id).set(feedback).await()
        return id
    }

    fun observeFeedback(uid: String): Flow<List<FeedbackMessage>> = callbackFlow {
        val registration = userDoc(uid).collection("feedback")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(FeedbackMessage::class.java) } ?: emptyList()
                trySend(list.sortedByDescending { it.submittedAtMillis })
            }
        awaitClose { registration.remove() }
    }
}
