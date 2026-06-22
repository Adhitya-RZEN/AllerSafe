package com.example.allersafe.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.allersafe.data.repository.AuthRepository
import com.example.allersafe.R
import com.example.allersafe.databinding.ActivityLoginpageBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginpageBinding
    private val authRepo = AuthRepository()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginpageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Tombol Login Email & Password
        binding.btnSubmitLogin.setOnClickListener {
            val input = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (input.isEmpty()) {
                binding.etEmail.error = "Email atau Username tidak boleh kosong"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password tidak boleh kosong"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            binding.btnSubmitLogin.isEnabled = false
            binding.btnSubmitLogin.text = "Memeriksa data..."

            lifecycleScope.launch {
                // Tentukan apakah ini email atau username (Case-Sensitive)
                val emailToLogin = if (input.contains("@")) {
                    input
                } else {
                    // Cari email berdasarkan username (case-sensitive)
                    val foundEmail = authRepo.getEmailByUsername(input)

                    if (foundEmail == null) {
                        Toast.makeText(this@LoginActivity, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                        binding.btnSubmitLogin.isEnabled = true
                        binding.btnSubmitLogin.text = "Login"
                        return@launch
                    }
                    foundEmail
                }

                val result = authRepo.login(emailToLogin, password)

                if (result.isSuccess) {
                    suksesLogin()
                } else {
                    Toast.makeText(this@LoginActivity, "Password salah atau gagal login", Toast.LENGTH_SHORT).show()
                    binding.btnSubmitLogin.isEnabled = true
                    binding.btnSubmitLogin.text = "Login"
                }
            }
        }

        // Tombol Login Google
        binding.btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = authRepo.getCurrentUser()
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In Dibatalkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        // 1. Tampilkan status loading ke user
        Toast.makeText(this, "Memproses login Google...", Toast.LENGTH_SHORT).show()

        // 2. Nonaktifkan tombol agar tidak bisa diklik berulang kali
        binding.btnGoogleLogin.isEnabled = false
        binding.btnSubmitLogin.isEnabled = false

        lifecycleScope.launch {
            val result = authRepo.loginWithGoogle(idToken)

            // 3. Aktifkan kembali tombol setelah proses selesai (baik sukses maupun gagal)
            binding.btnGoogleLogin.isEnabled = true
            binding.btnSubmitLogin.isEnabled = true

            if (result.isSuccess) {
                suksesLogin()
            } else {
                // Tangkap error jika koneksi gagal atau timeout
                val pesanError = result.exceptionOrNull()?.message ?: "Gagal terhubung ke server"
                Toast.makeText(this@LoginActivity, pesanError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun suksesLogin() {
        Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}