package com.example.allersafe.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.allersafe.R
import com.example.allersafe.data.repository.AuthRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BottomNavHelper {

    fun loadProfileIcon(context: Context, bottomNav: BottomNavigationView) {
        val sharedPref = context.getSharedPreferences("AllerSafeProfile", Context.MODE_PRIVATE)
        val cachedUrl = sharedPref.getString("PROFILE_IMAGE_URI", null)
        val authRepo = AuthRepository()

        if (!cachedUrl.isNullOrEmpty()) {
            // Jika memori cache sudah ada, langsung muat fotonya
            loadImageWithFallback(context, bottomNav, cachedUrl, authRepo)
        } else {
            // Jika memori kosong (Baru login atau Install ulang), tarik data dari Firestore secara diam-diam
            CoroutineScope(Dispatchers.IO).launch {
                val uid = authRepo.getCurrentUser()?.uid
                if (uid != null) {
                    val user = authRepo.getUserProfile(uid)
                    var firestoreUrl = user?.profileImageUrl

                    // Jika di Firestore juga tidak ada foto lokal, pakai foto bawaan Google
                    if (firestoreUrl.isNullOrEmpty()) {
                        firestoreUrl = authRepo.getCurrentUser()?.photoUrl?.toString() ?: ""
                    }

                    withContext(Dispatchers.Main) {
                        if (firestoreUrl.isNotEmpty()) {
                            // Simpan ke cache agar tidak perlu loading internet terus-menerus
                            sharedPref.edit().putString("PROFILE_IMAGE_URI", firestoreUrl).apply()
                            loadImageWithFallback(context, bottomNav, firestoreUrl, authRepo)
                        } else {
                            bottomNav.menu.findItem(R.id.nav_profile).setIcon(android.R.drawable.ic_menu_camera)
                        }
                    }
                }
            }
        }
    }

    private fun loadImageWithFallback(context: Context, bottomNav: BottomNavigationView, primaryUrl: String, authRepo: AuthRepository) {
        val profileMenuItem = bottomNav.menu.findItem(R.id.nav_profile)
        val fallbackGoogleUrl = authRepo.getCurrentUser()?.photoUrl?.toString()

        Glide.with(context)
            .asBitmap()
            .load(primaryUrl)
            .circleCrop()
            // LOGIKA TANPA STORAGE: Jika URL lokal rusak karena ganti HP, otomatis unduh foto Google
            .error(
                Glide.with(context)
                    .asBitmap()
                    .load(fallbackGoogleUrl)
                    .circleCrop()
                    .error(android.R.drawable.ic_menu_camera) // Jika Google gagal juga, beri ikon kamera
            )
            .into(object : CustomTarget<Bitmap>(80, 80) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    profileMenuItem.icon = resource.toDrawable(context.resources)
                    bottomNav.itemIconTintList = null

                    val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
                    val colors = intArrayOf(context.getColor(R.color.accent_primary), context.getColor(R.color.text_secondary))
                    val colorStateList = android.content.res.ColorStateList(states, colors)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bottomNav.menu.findItem(R.id.nav_home).iconTintList = colorStateList
                        bottomNav.menu.findItem(R.id.nav_settings).iconTintList = colorStateList
                        profileMenuItem.iconTintList = null
                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    profileMenuItem.icon = errorDrawable
                }
            })
    }
}