package com.example.smd_project

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FeedActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: VerificationAdapter

    private val items = mutableListOf<VerificationItem>()
    private val ref = Firebase.database.getReference("verifications")

    private var lastCount = 0  // to know if new signups arrived while viewing feed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        recycler = findViewById(R.id.recyclerVerifications)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = VerificationAdapter(
            items,
            onApproveClicked = { item -> updateStatus(item, "approved") },
            onRejectClicked = { item -> updateStatus(item, "rejected") }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        listenForPendingVerifications()
    }

    private fun listenForPendingVerifications() {
        ref.orderByChild("status").equalTo("pending")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<VerificationItem>()
                    for (child in snapshot.children) {
                        val name = child.child("name").getValue(String::class.java)
                        val email = child.child("email").getValue(String::class.java)
                        val status = child.child("status").getValue(String::class.java)
                        val userId = child.child("userId").getValue(String::class.java)
                        val createdAt = child.child("createdAt").getValue(Long::class.java)
                        val updatedAt = child.child("updatedAt").getValue(Long::class.java)

                        val item = VerificationItem(
                            id = child.key ?: "",
                            userId = userId,
                            name = name,
                            email = email,
                            status = status,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                        list.add(item)
                    }

                    adapter.updateData(list)
                    tvEmpty.visibility =
                        if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

                    // banker gets notification when new pending users appear while on this screen
                    if (list.size > lastCount) {
                        NotificationHelper.showNotification(
                            this@FeedActivity,
                            "New Signup",
                            "New user(s) waiting for verification."
                        )
                    }
                    lastCount = list.size
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@FeedActivity,
                        "Failed to load verifications: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun updateStatus(item: VerificationItem, newStatus: String) {
        if (item.id.isEmpty()) return

        val updates = mapOf(
            "status" to newStatus,
            "updatedAt" to System.currentTimeMillis()
        )

        ref.child(item.id).updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val msg = if (newStatus == "approved") {
                    "User ${item.email} has been verified."
                } else {
                    "User ${item.email} was disapproved."
                }
                NotificationHelper.showNotification(this, "Decision sent", msg)
            } else {
                Toast.makeText(
                    this,
                    "Failed to update status",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
