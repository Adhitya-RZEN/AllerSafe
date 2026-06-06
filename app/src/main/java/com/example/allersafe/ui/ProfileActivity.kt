package com.example.allersafe.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
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
    private val authRepo = AuthRepository()

    private val allergenList = mutableListOf<String>()
    private val selectedAllergens = mutableListOf<String>()
    private lateinit var allergenAdapter: AllergenPlainAdapter

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            profileImageUri = data?.data
            if (profileImageUri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        profileImageUri!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Glide.with(this).load(profileImageUri).circleCrop().into(binding.imgEditProfile)
                Glide.with(this).load(profileImageUri).circleCrop().into(binding.imgViewProfile)
                binding.imgEditProfile.imageTintList = null
                binding.imgViewProfile.imageTintList = null
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
        binding.btnBackToView.setOnClickListener {
            switchMode(isEditMode = false)
            loadProfileDataFromDatabase()
        }

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
            Toast.makeText(this, "Berhasil Keluar", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, FirstActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun switchMode(isEditMode: Boolean) {
        if (isEditMode) {
            binding.layoutViewMode.visibility = View.GONE
            binding.layoutEditMode.visibility = View.VISIBLE
        } else {
            binding.layoutEditMode.visibility = View.GONE
            binding.layoutViewMode.visibility = View.VISIBLE
        }
    }

    private fun getMasterCategory(input: String): String? {
        val lower = input.lowercase().trim()
        if (lower in listOf("susu", "keju", "milk", "dairy", "mentega", "yogurt", "laktosa")) return AllergenType.MILK.indonesianName
        if (lower in listOf("telur", "egg", "eggs", "albumin")) return AllergenType.EGG.indonesianName
        if (lower in listOf("gluten", "gandum", "wheat", "tepung terigu")) return AllergenType.WHEAT.indonesianName
        if (lower in listOf("kedelai", "soy", "soya", "tahu", "tempe", "kecap")) return AllergenType.SOY.indonesianName
        if (lower in listOf("kacang", "kacang tanah", "peanut")) return AllergenType.PEANUT.indonesianName
        if (lower in listOf("almond", "mete", "cashew", "walnut", "hazelnut", "tree nut")) return AllergenType.TREE_NUT.indonesianName
        if (lower in listOf("ikan", "fish", "salmon", "tuna")) return AllergenType.FISH.indonesianName
        if (lower in listOf("seafood", "udang", "kerang", "kepiting", "shellfish", "cumi")) return AllergenType.SHELLFISH.indonesianName
        return null
    }

    private fun setupAutoComplete() {
        binding.autoCompleteAllergen.threshold = 1
        binding.autoCompleteAllergen.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String
            val masterCategory = getMasterCategory(selectedItem) ?: selectedItem
            if (!selectedAllergens.contains(masterCategory)) {
                addChipToEdit(masterCategory)
                selectedAllergens.add(masterCategory)
            }
            binding.autoCompleteAllergen.text.clear()
        }

        binding.autoCompleteAllergen.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val enteredText = binding.autoCompleteAllergen.text.toString().trim()
                if (enteredText.isNotEmpty()) {
                    val masterCategory = getMasterCategory(enteredText) ?: enteredText
                    val validCategories = AllergenType.values().map { it.indonesianName }
                    if (validCategories.contains(masterCategory)) {
                        if (!selectedAllergens.contains(masterCategory)) {
                            addChipToEdit(masterCategory)
                            selectedAllergens.add(masterCategory)
                        }
                    } else {
                        Toast.makeText(this, "Alergen tidak dikenal", Toast.LENGTH_SHORT).show()
                    }
                }
                binding.autoCompleteAllergen.text.clear()
                true
            } else false
        }
    }

    private fun addChipToEdit(allergen: String) {
        val chip = Chip(this).apply {
            text = allergen
            isCloseIconVisible = true
            isClickable = true
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
            val adapter = ArrayAdapter(this@ProfileActivity, android.R.layout.simple_dropdown_item_1line, allergenList)
            binding.autoCompleteAllergen.setAdapter(adapter)

            val user = authRepo.getUserProfile(uid)
            if (user != null) {
                binding.tvViewUsername.text = user.displayName.ifEmpty { "Pengguna" }
                binding.tvViewEmail.text = user.email.ifEmpty { authRepo.getCurrentUser()?.email ?: "" }
                binding.etEditUsername.setText(user.displayName)
                binding.etEditDOB.setText(user.dob)
                binding.etEditGender.setText(user.gender)

                val url = user.profileImageUrl.ifEmpty { authRepo.getCurrentUser()?.photoUrl?.toString() ?: "" }
                if (url.isNotEmpty()) {
                    Glide.with(this@ProfileActivity).load(url).circleCrop().into(binding.imgViewProfile)
                    Glide.with(this@ProfileActivity).load(url).circleCrop().into(binding.imgEditProfile)
                }

                selectedAllergens.clear()
                binding.chipGroupEditAllergen.removeAllViews()
                val profile = user.allergenProfile
                if (profile.milk) selectedAllergens.add(AllergenType.MILK.indonesianName)
                if (profile.egg) selectedAllergens.add(AllergenType.EGG.indonesianName)
                if (profile.wheat) selectedAllergens.add(AllergenType.WHEAT.indonesianName)
                if (profile.soy) selectedAllergens.add(AllergenType.SOY.indonesianName)
                if (profile.peanut) selectedAllergens.add(AllergenType.PEANUT.indonesianName)
                if (profile.treeNut) selectedAllergens.add(AllergenType.TREE_NUT.indonesianName)
                if (profile.fish) selectedAllergens.add(AllergenType.FISH.indonesianName)
                if (profile.shellfish) selectedAllergens.add(AllergenType.SHELLFISH.indonesianName)

                if (selectedAllergens.isEmpty()) {
                    allergenAdapter.updateData(listOf("Belum ada alergen"))
                } else {
                    allergenAdapter.updateData(selectedAllergens.toList())
                    selectedAllergens.forEach { addChipToEdit(it) }
                }
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
            val result = authRepo.updateUserDetails(
                uid = uid,
                displayName = binding.etEditUsername.text.toString(),
                dob = binding.etEditDOB.text.toString(),
                gender = binding.etEditGender.text.toString(),
                profileImageUrl = profileImageUri?.toString() ?: "",
                allergenProfile = newProfile
            )
            binding.btnSaveProfile.isEnabled = true
            if (result.isSuccess) {
                switchMode(false)
                loadProfileDataFromDatabase()
                Toast.makeText(this@ProfileActivity, "Profil disimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    // Menambahkan penghilang animasi untuk perpindahan ke Home
                    if (Build.VERSION.SDK_INT >= 34) {
                        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    // Menambahkan penghilang animasi untuk perpindahan ke Settings
                    if (Build.VERSION.SDK_INT >= 34) {
                        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNavHelper.loadProfileIcon(this, binding.bottomNavigation)
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
    }
}
