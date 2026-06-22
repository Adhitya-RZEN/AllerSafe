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
        val authRepo = AuthRepository()

        CoroutineScope(Dispatchers.IO).launch {
            val uid = authRepo.getCurrentUser()?.uid
            if (uid != null) {
                // Selalu ambil data terbaru dari Firestore, jangan pakai SharedPreferences
                val user = authRepo.getUserProfile(uid)
                val imageUrl = user?.profileImageUrl?.ifEmpty { null }
                    ?: authRepo.getCurrentUser()?.photoUrl?.toString()

                withContext(Dispatchers.Main) {
                    if (!imageUrl.isNullOrEmpty()) {
                        loadImageWithFallback(context, bottomNav, imageUrl)
                    } else {
                        bottomNav.menu.findItem(R.id.nav_profile).setIcon(android.R.drawable.ic_menu_camera)
                    }
                }
            }
        }
    }

    private fun loadImageWithFallback(context: Context, bottomNav: BottomNavigationView, url: String) {
        val profileMenuItem = bottomNav.menu.findItem(R.id.nav_profile)

        Glide.with(context)
            .asBitmap()
            .load(url)
            .circleCrop()
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(object : CustomTarget<Bitmap>(80, 80) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    profileMenuItem.icon = resource.toDrawable(context.resources)
                    bottomNav.itemIconTintList = null
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }
}