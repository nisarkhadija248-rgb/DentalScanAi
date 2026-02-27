package com.dentalscan

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.dentalscan.data.AppDatabase
import com.dentalscan.data.ProfileEntity
import com.dentalscan.utils.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditProfileActivity : BaseActivity() {

    private lateinit var formTitle: TextView
    private lateinit var avatarPreview: TextView
    private lateinit var editName: EditText
    private lateinit var spinnerRelationship: Spinner
    private lateinit var editAge: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var btnDeleteProfile: Button

    private var editingProfile: ProfileEntity? = null

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_profile)

        initViews()
        setupRelationshipSpinner()
        setupListeners()

        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)
        if (profileId != -1L) {
            loadProfileForEditing(profileId)
        }
    }

    private fun initViews() {
        formTitle = findViewById(R.id.formTitle)
        avatarPreview = findViewById(R.id.avatarPreview)
        editName = findViewById(R.id.editName)
        spinnerRelationship = findViewById(R.id.spinnerRelationship)
        editAge = findViewById(R.id.editAge)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnDeleteProfile = findViewById(R.id.btnDeleteProfile)
    }

    private fun setupRelationshipSpinner() {
        val relationships = listOf(
            "Self", "Dad", "Mom", "Son",
            "Daughter", "Brother", "Sister",
            "Grandfather", "Grandmother", "Other"
        )
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            relationships
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRelationship.adapter = adapter
    }

    private fun setupListeners() {
        // Update avatar initials as name is typed
        editName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateAvatarPreview(s.toString())
            }
        })

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        btnDeleteProfile.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun updateAvatarPreview(name: String) {
        val initials = name
            .split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
        avatarPreview.text = if (initials.isEmpty()) "?" else initials
    }

    private fun loadProfileForEditing(profileId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val profile = AppDatabase.getDatabase(this@AddEditProfileActivity)
                .profileDao()
                .getProfileById(profileId)

            withContext(Dispatchers.Main) {
                if (profile != null) {
                    editingProfile = profile
                    formTitle.text = "Edit Profile"
                    editName.setText(profile.name)
                    editAge.setText(profile.age.toString())

                    val relationships = listOf(
                        "Self", "Dad", "Mom", "Son",
                        "Daughter", "Brother", "Sister",
                        "Grandfather", "Grandmother", "Other"
                    )
                    val index = relationships.indexOf(profile.relationship)
                    if (index >= 0) spinnerRelationship.setSelection(index)

                    updateAvatarPreview(profile.name)
                    btnDeleteProfile.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun saveProfile() {
        val name = editName.text.toString().trim()
        val ageText = editAge.text.toString().trim()
        val relationship = spinnerRelationship.selectedItem.toString()

        if (name.isEmpty()) {
            editName.error = "Name is required"
            return
        }
        if (ageText.isEmpty()) {
            editAge.error = "Age is required"
            return
        }
        val age = ageText.toIntOrNull()
        if (age == null || age < 1 || age > 120) {
            editAge.error = "Please enter a valid age"
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(this@AddEditProfileActivity)

            if (editingProfile != null) {
                val updated = editingProfile!!.copy(
                    name = name,
                    relationship = relationship,
                    age = age
                )
                database.profileDao().updateProfile(updated)
            } else {
                val newProfile = ProfileEntity(
                    name = name,
                    relationship = relationship,
                    age = age
                )
                database.profileDao().insertProfile(newProfile)
            }

            withContext(Dispatchers.Main) {
                val isUrdu = LanguageManager.isUrdu(this@AddEditProfileActivity)
                Toast.makeText(
                    this@AddEditProfileActivity,
                    if (isUrdu) "پروفائل محفوظ ہوگئی" else "Profile saved",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun showDeleteConfirmation() {
        val isUrdu = LanguageManager.isUrdu(this)
        AlertDialog.Builder(this)
            .setTitle(if (isUrdu) "پروفائل حذف کریں" else "Delete Profile")
            .setMessage(if (isUrdu) "کیا آپ واقعی یہ پروفائل حذف کرنا چاہتے ہیں؟" else "Are you sure you want to delete this profile?")
            .setPositiveButton(if (isUrdu) "ہاں" else "Yes") { _, _ -> deleteProfile() }
            .setNegativeButton(if (isUrdu) "نہیں" else "No", null)
            .show()
    }

    private fun deleteProfile() {
        val profile = editingProfile ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@AddEditProfileActivity)
                .profileDao()
                .deleteProfile(profile)
            withContext(Dispatchers.Main) {
                val isUrdu = LanguageManager.isUrdu(this@AddEditProfileActivity)
                Toast.makeText(
                    this@AddEditProfileActivity,
                    if (isUrdu) "پروفائل حذف کر دی گئی" else "Profile deleted",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}