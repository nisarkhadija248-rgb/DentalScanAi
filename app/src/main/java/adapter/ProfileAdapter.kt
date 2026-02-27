package com.dentalscan.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dentalscan.R
import com.dentalscan.data.ProfileEntity

class ProfileAdapter(
    private val onProfileClick: (ProfileEntity) -> Unit,
    private val onEditClick: (ProfileEntity) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    private var profiles: List<ProfileEntity> = emptyList()
    private var activeProfileId: Long = -1L

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarText: TextView = view.findViewById(R.id.avatarText)
        val profileName: TextView = view.findViewById(R.id.profileName)
        val profileRelationship: TextView = view.findViewById(R.id.profileRelationship)
        val profileAge: TextView = view.findViewById(R.id.profileAge)
        val activeIndicator: TextView = view.findViewById(R.id.activeIndicator)
        val btnEditProfile: ImageButton = view.findViewById(R.id.btnEditProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles[position]

        // Initials avatar
        val initials = profile.name
            .split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
        holder.avatarText.text = initials

        holder.profileName.text = profile.name
        holder.profileRelationship.text = profile.relationship
        holder.profileAge.text = "Age: ${profile.age}"

        // Active indicator
        holder.activeIndicator.visibility =
            if (profile.id == activeProfileId) View.VISIBLE else View.GONE

        // Tap card = set as active profile
        holder.itemView.setOnClickListener {
            onProfileClick(profile)
        }

        // Tap edit button = open edit screen
        holder.btnEditProfile.setOnClickListener {
            onEditClick(profile)
        }
    }

    override fun getItemCount() = profiles.size

    fun submitList(newProfiles: List<ProfileEntity>) {
        profiles = newProfiles
        notifyDataSetChanged()
    }

    fun setActiveProfileId(id: Long) {
        activeProfileId = id
        notifyDataSetChanged()
    }
}