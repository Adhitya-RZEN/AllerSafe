package com.example.allersafe.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.allersafe.R
import com.example.allersafe.adapter.AllergenPlainAdapter
import com.example.allersafe.data.model.AllergenProfile
import com.example.allersafe.data.model.AllergenType
import com.example.allersafe.data.repository.AuthRepository
import com.example.allersafe.databinding.ActivityProfileBinding
import com.example.allersafe.engine.BottomNavHelper
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var profileImageUri: Uri? = null
    // Menambahkan variabel untuk menyimpan URL foto yang sudah ada di database
    private var currentProfileImageUrl: String = ""
    private val authRepo = AuthRepository()

    private val allergenList = mutableListOf<String>()
    private val selectedAllergens = mutableListOf<String>()
    private lateinit var allergenAdapter: AllergenPlainAdapter

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            profileImageUri = data?.data
            if (profileImageUri != null) {
                loadProfileImage(profileImageUri.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        switchMode(isEditMode = false)

        allergenAdapter = AllergenPlainAdapter(emptyList())
        binding.rvViewAllergens.layoutManager = LinearLayoutManager(this)
        binding.rvViewAllergens.adapter = allergenAdapter

        setupAutoComplete()
        loadProfileDataFromDatabase()
        setupBottomNavigation()

        binding.btnGoToEdit.setOnClickListener { switchMode(isEditMode = true) }
        binding.btnBackToView.setOnClickListener { switchMode(isEditMode = false) }

        binding.imgEditProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        binding.btnSaveProfile.setOnClickListener { saveProfileDataToDatabase() }

        binding.btnLogout.setOnClickListener {
            authRepo.logout()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadProfileImage(url: String) {
        val requestListener = object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                android.util.Log.e("GlideError", "Gagal load: ${e?.message}")
                return false
            }
            override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                return false
            }
        }

        Glide.with(this)
            .load(url)
            .circleCrop()
            .listener(requestListener)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(binding.imgViewProfile)

        Glide.with(this)
            .load(url)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(binding.imgEditProfile)
    }

    private fun switchMode(isEditMode: Boolean) {
        binding.layoutViewMode.visibility = if (isEditMode) View.GONE else View.VISIBLE
        binding.layoutEditMode.visibility = if (isEditMode) View.VISIBLE else View.GONE
    }

    private fun setupAutoComplete() {
        binding.autoCompleteAllergen.threshold = 1
        binding.autoCompleteAllergen.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String
            val masterCategory = AllergenType.entries.find { it.indonesianName == selectedItem }?.indonesianName ?: selectedItem
            if (!selectedAllergens.contains(masterCategory)) {
                addChipToEdit(masterCategory)
                selectedAllergens.add(masterCategory)
            }
            binding.autoCompleteAllergen.text.clear()
        }
    }

    private fun addChipToEdit(allergen: String) {
        val chip = Chip(this).apply {
            text = allergen
            isCloseIconVisible = true
            setChipBackgroundColorResource(R.color.accent_primary)
            setTextColor(getColor(R.color.bg_main))
            setCloseIconTintResource(R.color.bg_main)
            setOnCloseIconClickListener {
                binding.chipGroupEditAllergen.removeView(this)
                selectedAllergens.remove(allergen)
            }
        }
        binding.chipGroupEditAllergen.addView(chip)
    }

    private fun loadProfileDataFromDatabase() {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            val listFromDb = authRepo.getMasterAllergens()
            allergenList.clear()
            allergenList.addAll(listFromDb)
            binding.autoCompleteAllergen.setAdapter(ArrayAdapter(this@ProfileActivity, android.R.layout.simple_dropdown_item_1line, allergenList))

            val user = authRepo.getUserProfile(uid)
            if (user != null) {
                // Simpan URL foto saat ini ke variabel penampung
                currentProfileImageUrl = user.profileImageUrl

                binding.tvViewUsername.text = user.displayName.ifEmpty { "Pengguna" }
                binding.tvViewEmail.text = user.email
                binding.etEditUsername.setText(user.displayName)
                binding.etEditDOB.setText(user.dob)
                binding.etEditGender.setText(user.gender)

                if (user.profileImageUrl.isNotEmpty()) loadProfileImage(user.profileImageUrl)

                selectedAllergens.clear()
                binding.chipGroupEditAllergen.removeAllViews()
                val p = user.allergenProfile
                if (p.milk) selectedAllergens.add(AllergenType.MILK.indonesianName)
                if (p.egg) selectedAllergens.add(AllergenType.EGG.indonesianName)
                if (p.wheat) selectedAllergens.add(AllergenType.WHEAT.indonesianName)
                if (p.soy) selectedAllergens.add(AllergenType.SOY.indonesianName)
                if (p.peanut) selectedAllergens.add(AllergenType.PEANUT.indonesianName)
                if (p.treeNut) selectedAllergens.add(AllergenType.TREE_NUT.indonesianName)
                if (p.fish) selectedAllergens.add(AllergenType.FISH.indonesianName)
                if (p.shellfish) selectedAllergens.add(AllergenType.SHELLFISH.indonesianName)

                allergenAdapter.updateData(selectedAllergens.ifEmpty { listOf("Belum ada alergen") })
                selectedAllergens.forEach { addChipToEdit(it) }
            }
        }
    }

    private fun saveProfileDataToDatabase() {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        val newProfile = AllergenProfile(
            milk = selectedAllergens.contains(AllergenType.MILK.indonesianName),
            egg = selectedAllergens.contains(AllergenType.EGG.indonesianName),
            wheat = selectedAllergens.contains(AllergenType.WHEAT.indonesianName),
            soy = selectedAllergens.contains(AllergenType.SOY.indonesianName),
            peanut = selectedAllergens.contains(AllergenType.PEANUT.indonesianName),
            treeNut = selectedAllergens.contains(AllergenType.TREE_NUT.indonesianName),
            fish = selectedAllergens.contains(AllergenType.FISH.indonesianName),
            shellfish = selectedAllergens.contains(AllergenType.SHELLFISH.indonesianName)
        )

        binding.btnSaveProfile.isEnabled = false
        lifecycleScope.launch {
            // PERBAIKAN: Gunakan currentProfileImageUrl jika profileImageUri null
            val finalImageUrl = profileImageUri?.toString() ?: currentProfileImageUrl

            val result = authRepo.updateUserDetails(
                uid = uid,
                displayName = binding.etEditUsername.text.toString(),
                dob = binding.etEditDOB.text.toString(),
                gender = binding.etEditGender.text.toString(),
                profileImageUrl = finalImageUrl,
                allergenProfile = newProfile
            )

            binding.btnSaveProfile.isEnabled = true
            if (result.isSuccess) {
                switchMode(false)
                loadProfileDataFromDatabase()
                BottomNavHelper.loadProfileIcon(this@ProfileActivity, binding.bottomNavigation)
                Toast.makeText(this@ProfileActivity, "Profil disimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNavHelper.loadProfileIcon(this, binding.bottomNavigation)
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
    }
}