package com.example.smd_project

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.FirebaseDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Warm up Firestore
        val fs = Firebase.firestore

        // Warm up Realtime Database (KTX)
        try {
            val rdb = Firebase.database
            // Optionally set persistence or other config here if desired:
            // FirebaseDatabase.getInstance().setPersistenceEnabled(true) // note: call before any DB usage
        } catch (ex: Exception) {
            // ignore warming errors; they will surface later when used
            ex.printStackTrace()
        }
    }
}
