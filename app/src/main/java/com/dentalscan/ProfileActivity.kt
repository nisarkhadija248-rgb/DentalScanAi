package com.dentalscan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dentalscan.adapter.ProfileAdapter
import com.dentalscan.data.AppDatabase
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.ProfileManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {

    private lateinit var titleText: TextView
    private lateinit var activeProfileText: TextView
    private lateinit var profileRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var fabAddProfile: FloatingActionButton
    private lateinit var profileAdapter: ProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadProfiles()
    }

    override fun onResume() {
        super.onResume()
        // Reload when returning from AddEditProfileActivity
        loadProfiles()
    }

    private fun initViews() {
        titleText = findViewById(R.id.titleText)
        activeProfileText = findViewById(R.id.activeProfileText)
        profileRecyclerView = findViewById(R.id.profileRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        fabAddProfile = findViewById(R.id.fabAddProfile)

        val isUrdu = LanguageManager.isUrdu(this)
        titleText.text = if (isUrdu) "خاندانی پروفائلز" else "Family Profiles"
    }

    private fun setupRecyclerView() {
        profileAdapter = ProfileAdapter(
            onProfileClick = { profile ->
                ProfileManager.setActiveProfile(this, profile.id, profile.name)
                profileAdapter.setActiveProfileId(profile.id)
                updateActiveProfileText()

                val isUrdu = LanguageManager.isUrdu(this)
                Toast.makeText(
                    this,
                    if (isUrdu) "${profile.name} کو فعال پروفائل بنا دیا گیا"
                    else "${profile.name} is now the active profile",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onEditClick = { profile ->
                val intent = Intent(this, AddEditProfileActivity::class.java)
                intent.putExtra(AddEditProfileActivity.EXTRA_PROFILE_ID, profile.id)
                startActivity(intent)
            }
        )
        profileRecyclerView.layoutManager = LinearLayoutManager(this)
        profileRecyclerView.adapter = profileAdapter
    }

    private fun setupListeners() {
        fabAddProfile.setOnClickListener {
            startActivity(Intent(this, AddEditProfileActivity::class.java))
        }
    }

    private fun loadProfiles() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@ProfileActivity)
            val profiles = database.profileDao().getAllProfiles().first()

            val activeId = ProfileManager.getActiveProfileId(this@ProfileActivity)
            profileAdapter.setActiveProfileId(activeId)

            if (profiles.isEmpty()) {
                emptyText.visibility = View.VISIBLE
                profileRecyclerView.visibility = View.GONE
            } else {
                emptyText.visibility = View.GONE
                profileRecyclerView.visibility = View.VISIBLE
                profileAdapter.submitList(profiles)
            }

            updateActiveProfileText()
        }
    }

    private fun updateActiveProfileText() {
        val isUrdu = LanguageManager.isUrdu(this)
        val activeName = ProfileManager.getActiveProfileName(this)
        activeProfileText.text = if (isUrdu)
            "فعال: $activeName"
        else
            "Active: $activeName"
    }
}