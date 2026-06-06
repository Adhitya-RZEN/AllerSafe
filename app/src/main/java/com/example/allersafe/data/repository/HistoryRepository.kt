package com.example.allersafe.data.repository

import com.example.allersafe.data.model.ScanResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class HistoryRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val COLLECTION_USERS = "users"
        // UBAH INI MENJADI "history" AGAR SAMA DENGAN ScanRepository
        private const val SUBCOLLECTION_HISTORY = "history"
        private const val PAGE_SIZE = 20L
    }

    suspend fun getScanHistory(uid: String): Result<List<ScanResult>> = runCatching {
        val snapshot = db.collection(COLLECTION_USERS).document(uid).collection(SUBCOLLECTION_HISTORY)
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(PAGE_SIZE).get().await()
        snapshot.documents.mapNotNull { doc ->
            runCatching { ScanResult.Companion.fromMap(doc.id, doc.data ?: emptyMap()) }.getOrNull()
        }
    }

    // --- FUNGSI BARU: MENGHAPUS SEMUA RIWAYAT SCAN USER ---
    suspend fun clearScanHistory(uid: String): Result<Unit> = runCatching {
        val collectionRef = db.collection(COLLECTION_USERS).document(uid).collection(SUBCOLLECTION_HISTORY)
        val snapshot = collectionRef.get().await()

        // Menggunakan WriteBatch untuk menghapus banyak dokumen sekaligus agar efisien
        val batch = db.batch()
        for (document in snapshot.documents) {
            batch.delete(document.reference)
        }
        batch.commit().await()
    }
}