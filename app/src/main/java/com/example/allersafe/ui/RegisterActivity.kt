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

    // Diubah menjadi List Dinamis
    private val allergenList = mutableListOf<String>()
    private val selectedAllergens = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegispageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mengambil daftar alergen dari database
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

                // Logika Pemetaan Pintar (Menggunakan contains agar lebih dinamis)
                val profile = AllergenProfile(
                    milk = selectedAllergens.any { it.contains("Susu", true) || it.contains("Keju", true) },
                    egg = selectedAllergens.any { it.contains("Telur", true) },
                    wheat = selectedAllergens.any { it.contains("Gluten", true) || it.contains("Gandum", true) },
                    shellfish = selectedAllergens.any { it.contains("Seafood", true) || it.contains("Udang", true) },
                    peanut = selectedAllergens.any { it.contains("Kacang", true) }
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

            // Set adapter setelah data dari database selesai diunduh
            val adapter = ArrayAdapter(this@RegisterActivity, android.R.layout.simple_dropdown_item_1line, allergenList)
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