package com.example.allersafe.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.allersafe.R
import com.example.allersafe.data.model.AllergenProfile
import com.example.allersafe.data.repository.AuthRepository
import com.example.allersafe.databinding.ActivityRegispageBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegispageBinding
    private val authRepo = AuthRepository()

    private val allergenList = mutableListOf<String>()
    private val selectedAllergens = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegispageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadMasterAllergens()

        binding.btnSubmitDaftar.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val inputUsername = binding.etUsername.text.toString().trim()
            val username = inputUsername.ifEmpty { email.substringBefore("@") }

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password != confirmPassword) {
                    Toast.makeText(this, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (password.length < 6) {
                    Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // BUG #2 FIX: Semua 8 jenis alergen sekarang dipetakan dengan benar.
                // Sebelumnya SOY, TREE_NUT, dan FISH tidak disertakan sehingga
                // profil alergi pengguna yang memilih jenis tersebut tidak pernah tersimpan.
                val profile = AllergenProfile(
                    milk     = selectedAllergens.any {
                        it.contains("Susu", ignoreCase = true) ||
                                it.contains("Keju", ignoreCase = true) ||
                                it.contains("Dairy", ignoreCase = true)
                    },
                    egg      = selectedAllergens.any {
                        it.contains("Telur", ignoreCase = true) ||
                                it.contains("Egg", ignoreCase = true)
                    },
                    wheat    = selectedAllergens.any {
                        it.contains("Gluten", ignoreCase = true) ||
                                it.contains("Gandum", ignoreCase = true) ||
                                it.contains("Wheat", ignoreCase = true) ||
                                it.contains("Tepung", ignoreCase = true)
                    },
                    // --- PERBAIKAN: SOY sebelumnya tidak ada sama sekali ---
                    soy      = selectedAllergens.any {
                        it.contains("Kedelai", ignoreCase = true) ||
                                it.contains("Soy", ignoreCase = true) ||
                                it.contains("Tahu", ignoreCase = true) ||
                                it.contains("Tempe", ignoreCase = true) ||
                                it.contains("Kecap", ignoreCase = true)
                    },
                    peanut   = selectedAllergens.any {
                        it.contains("Kacang", ignoreCase = true) ||
                                it.contains("Peanut", ignoreCase = true)
                    },
                    // --- PERBAIKAN: TREE_NUT sebelumnya tidak ada sama sekali ---
                    treeNut  = selectedAllergens.any {
                        it.contains("Almond", ignoreCase = true) ||
                                it.contains("Mete", ignoreCase = true) ||
                                it.contains("Cashew", ignoreCase = true) ||
                                it.contains("Walnut", ignoreCase = true) ||
                                it.contains("Hazelnut", ignoreCase = true) ||
                                it.contains("Kacang Pohon", ignoreCase = true) ||
                                it.contains("Tree Nut", ignoreCase = true) ||
                                it.contains("Pistachio", ignoreCase = true)
                    },
                    // --- PERBAIKAN: FISH sebelumnya tidak ada sama sekali ---
                    fish     = selectedAllergens.any {
                        it.contains("Ikan", ignoreCase = true) ||
                                it.contains("Fish", ignoreCase = true) ||
                                it.contains("Salmon", ignoreCase = true) ||
                                it.contains("Tuna", ignoreCase = true) ||
                                it.contains("Teri", ignoreCase = true)
                    },
                    shellfish = selectedAllergens.any {
                        it.contains("Seafood", ignoreCase = true) ||
                                it.contains("Udang", ignoreCase = true) ||
                                it.contains("Kerang", ignoreCase = true) ||
                                it.contains("Kepiting", ignoreCase = true) ||
                                it.contains("Cumi", ignoreCase = true)
                    }
                )

                Toast.makeText(this, "Memproses pendaftaran...", Toast.LENGTH_SHORT).show()
                binding.btnSubmitDaftar.isEnabled = false

                lifecycleScope.launch {
                    val result = authRepo.register(email, password, username, profile)

                    if (result.isSuccess) {
                        Toast.makeText(this@RegisterActivity, "Berhasil Daftar!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    } else {
                        val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Terjadi kesalahan"
                        Toast.makeText(this@RegisterActivity, "Gagal Daftar: $errorMsg", Toast.LENGTH_LONG).show()
                        binding.btnSubmitDaftar.isEnabled = true
                    }
                }
            } else {
                Toast.makeText(this, "Harap isi semua form", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMasterAllergens() {
        lifecycleScope.launch {
            val listFromDb = authRepo.getMasterAllergens()
            allergenList.clear()
            allergenList.addAll(listFromDb)

            val adapter = ArrayAdapter(
                this@RegisterActivity,
                android.R.layout.simple_dropdown_item_1line,
                allergenList
            )
            binding.autoCompleteAllergen.setAdapter(adapter)

            binding.autoCompleteAllergen.setOnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position) as String
                if (!selectedAllergens.contains(selectedItem)) {
                    addChip(selectedItem)
                    selectedAllergens.add(selectedItem)
                }
                binding.autoCompleteAllergen.text.clear()
            }
        }
    }

    private fun addChip(allergen: String) {
        val chip = Chip(this).apply {
            text = allergen
            isCloseIconVisible = true
            isClickable = true
            isCheckable = false
            setChipBackgroundColorResource(R.color.accent_primary)
            setTextColor(getColor(R.color.bg_main))
            setCloseIconTintResource(R.color.bg_main)
            setOnCloseIconClickListener {
                binding.chipGroupAllergen.removeView(this)
                selectedAllergens.remove(allergen)
            }
        }
        binding.chipGroupAllergen.addView(chip)
    }
}