package com.example.allersafe.ui.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.allersafe.R
import com.example.allersafe.data.model.AllergenProfile
import com.example.allersafe.data.model.AllergenType
import com.example.allersafe.data.repository.AuthRepository
import com.example.allersafe.databinding.FragmentProfileBinding
import com.example.allersafe.ui.FirstActivity
import com.example.allersafe.ui.MainActivity
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var profileImageUri: Uri? = null
    private var currentProfileImageUrl: String = "" // Simpan URL foto lama di sini
    private val authRepo = AuthRepository()
    private val allergenList = mutableListOf<String>()
    private val selectedAllergens = mutableListOf<String>()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            profileImageUri = result.data?.data
            profileImageUri?.let { uri ->
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { e.printStackTrace() }
                Glide.with(this).load(uri).circleCrop().into(binding.imgEditProfile)
                Glide.with(this).load(uri).circleCrop().into(binding.imgViewProfile)
                binding.imgEditProfile.imageTintList = null
                binding.imgViewProfile.imageTintList = null
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchMode(isEditMode = false)

        setupAutoComplete()
        loadProfileDataFromDatabase()

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
            Toast.makeText(requireContext(), "Berhasil Keluar", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), FirstActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.refreshProfileIcon()
    }

    private fun switchMode(isEditMode: Boolean) {
        binding.layoutViewMode.visibility = if (isEditMode) View.GONE else View.VISIBLE
        binding.layoutEditMode.visibility = if (isEditMode) View.VISIBLE else View.GONE
    }

    private fun getMasterCategory(input: String): String? {
        val lower = input.lowercase().trim()
        if (lower in listOf("susu","keju","milk","dairy","mentega","yogurt","laktosa")) return AllergenType.MILK.indonesianName
        if (lower in listOf("telur","egg","eggs","albumin")) return AllergenType.EGG.indonesianName
        if (lower in listOf("gluten","gandum","wheat","tepung terigu")) return AllergenType.WHEAT.indonesianName
        if (lower in listOf("kedelai","soy","soya","tahu","tempe","kecap")) return AllergenType.SOY.indonesianName
        if (lower in listOf("kacang","kacang tanah","peanut")) return AllergenType.PEANUT.indonesianName
        if (lower in listOf("almond","mete","cashew","walnut","hazelnut","tree nut")) return AllergenType.TREE_NUT.indonesianName
        if (lower in listOf("ikan","fish","salmon","tuna")) return AllergenType.FISH.indonesianName
        if (lower in listOf("seafood","udang","kerang","kepiting","shellfish","cumi")) return AllergenType.SHELLFISH.indonesianName
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
                        Toast.makeText(requireContext(), "Alergen tidak dikenal", Toast.LENGTH_SHORT).show()
                    }
                }
                binding.autoCompleteAllergen.text.clear()
                true
            } else false
        }
    }

    private fun addChipToEdit(allergen: String) {
        val chip = Chip(requireContext()).apply {
            text = allergen
            isCloseIconVisible = true
            isClickable = true
            setChipBackgroundColorResource(R.color.accent_primary)
            setTextColor(requireContext().getColor(R.color.bg_main))
            setCloseIconTintResource(R.color.bg_main)
            setOnCloseIconClickListener {
                binding.chipGroupEditAllergen.removeView(this)
                selectedAllergens.remove(allergen)
            }
        }
        binding.chipGroupEditAllergen.addView(chip)
    }

    private fun addChipToView(allergen: String) {
        val chip = Chip(requireContext()).apply {
            text = allergen
            isClickable = false
            setChipBackgroundColorResource(R.color.bg_main)
            setTextColor(requireContext().getColor(R.color.text_primary))
        }
        binding.cgViewAllergens.addView(chip)
    }

    private fun loadProfileDataFromDatabase() {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val listFromDb = authRepo.getMasterAllergens()
            allergenList.clear()
            allergenList.addAll(listFromDb)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, allergenList)
            binding.autoCompleteAllergen.setAdapter(adapter)

            val user = authRepo.getUserProfile(uid)
            if (user != null) {
                binding.tvViewUsername.text = user.displayName.ifEmpty { "Pengguna" }
                binding.tvViewEmail.text = user.email.ifEmpty { authRepo.getCurrentUser()?.email ?: "" }
                binding.etEditUsername.setText(user.displayName)
                binding.etEditDOB.setText(user.dob)
                binding.etEditGender.setText(user.gender)

                // Simpan URL foto yang ada sekarang ke currentProfileImageUrl
                currentProfileImageUrl = user.profileImageUrl.ifEmpty { authRepo.getCurrentUser()?.photoUrl?.toString() ?: "" }
                
                if (currentProfileImageUrl.isNotEmpty()) {
                    Glide.with(this@ProfileFragment).load(currentProfileImageUrl).circleCrop().into(binding.imgViewProfile)
                    Glide.with(this@ProfileFragment).load(currentProfileImageUrl).circleCrop().into(binding.imgEditProfile)
                    binding.imgViewProfile.imageTintList = null
                    binding.imgEditProfile.imageTintList = null
                }

                selectedAllergens.clear()
                binding.chipGroupEditAllergen.removeAllViews()
                binding.cgViewAllergens.removeAllViews()
                
                val profile = user.allergenProfile
                if (profile.milk)      selectedAllergens.add(AllergenType.MILK.indonesianName)
                if (profile.egg)       selectedAllergens.add(AllergenType.EGG.indonesianName)
                if (profile.wheat)     selectedAllergens.add(AllergenType.WHEAT.indonesianName)
                if (profile.soy)       selectedAllergens.add(AllergenType.SOY.indonesianName)
                if (profile.peanut)    selectedAllergens.add(AllergenType.PEANUT.indonesianName)
                if (profile.treeNut)   selectedAllergens.add(AllergenType.TREE_NUT.indonesianName)
                if (profile.fish)      selectedAllergens.add(AllergenType.FISH.indonesianName)
                if (profile.shellfish) selectedAllergens.add(AllergenType.SHELLFISH.indonesianName)

                binding.tvAllergenCount.text = if (selectedAllergens.isEmpty()) {
                    "Tidak ada alergen aktif"
                } else {
                    "${selectedAllergens.size} Alergen Aktif"
                }

                if (selectedAllergens.isNotEmpty()) {
                    selectedAllergens.forEach { 
                        addChipToView(it)
                        addChipToEdit(it)
                    }
                }
            }
        }
    }

    private fun saveProfileDataToDatabase() {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        val newProfile = AllergenProfile(
            milk      = selectedAllergens.contains(AllergenType.MILK.indonesianName),
            egg       = selectedAllergens.contains(AllergenType.EGG.indonesianName),
            wheat     = selectedAllergens.contains(AllergenType.WHEAT.indonesianName),
            soy       = selectedAllergens.contains(AllergenType.SOY.indonesianName),
            peanut    = selectedAllergens.contains(AllergenType.PEANUT.indonesianName),
            treeNut   = selectedAllergens.contains(AllergenType.TREE_NUT.indonesianName),
            fish      = selectedAllergens.contains(AllergenType.FISH.indonesianName),
            shellfish = selectedAllergens.contains(AllergenType.SHELLFISH.indonesianName)
        )
        binding.btnSaveProfile.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val result = authRepo.updateUserDetails(
                uid = uid,
                displayName = binding.etEditUsername.text.toString(),
                dob = binding.etEditDOB.text.toString(),
                gender = binding.etEditGender.text.toString(),
                // Gunakan profileImageUri jika ada (foto baru dipilih), jika tidak gunakan currentProfileImageUrl
                profileImageUrl = profileImageUri?.toString() ?: currentProfileImageUrl,
                allergenProfile = newProfile
            )
            binding.btnSaveProfile.isEnabled = true
            if (result.isSuccess) {
                switchMode(false)
                loadProfileDataFromDatabase()
                (activity as? MainActivity)?.refreshProfileIcon()
                Toast.makeText(requireContext(), "Profil disimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}