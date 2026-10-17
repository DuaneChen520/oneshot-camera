package dev.oneshot.camera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

object GalleryLauncher {

    private val KNOWN_GALLERY_PACKAGES = listOf(
        "com.miui.gallery",                // Xiaomi / HyperOS / MIUI
        "com.google.android.apps.photos",  // Google Pixel / Android One
        "com.sec.android.gallery3d",       // Samsung OneUI
        "com.huawei.photos",               // Huawei HarmonyOS
        "com.hihonor.photos",              // Honor MagicOS
        "com.coloros.gallery3d",           // OPPO ColorOS
        "com.oneplus.gallery",             // OnePlus
        "com.vivo.gallery",                // Vivo OriginOS
        "com.transsion.gallery",           // Transsion
        "com.android.gallery3d",           // AOSP Gallery
        "com.android.gallery"              // Generic Gallery
    )

    /**
     * Resolves the primary gallery package on the device.
     */
    fun resolveGalleryPackage(context: Context): String? {
        val pm = context.packageManager

        // 1. Check system default for standard CATEGORY_APP_GALLERY
        runCatching {
            val mainGalleryIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)
            val resolveInfo = pm.resolveActivity(mainGalleryIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val pkg = resolveInfo?.activityInfo?.packageName
            if (!pkg.isNullOrEmpty() && pkg != "android" && !pkg.contains("ResolverActivity")) {
                return pkg
            }
        }

        // 2. Check installed known OEM gallery packages
        for (pkg in KNOWN_GALLERY_PACKAGES) {
            runCatching {
                pm.getPackageInfo(pkg, 0)
                return pkg
            }
        }

        // 3. Fallback: match any package name containing "gallery" or "photos"
        runCatching {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
            }
            val list = pm.queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val match = list.firstOrNull { ri ->
                val p = ri.activityInfo.packageName.lowercase()
                p.contains("gallery") || p.contains("photos")
            }
            if (match != null) {
                return match.activityInfo.packageName
            }
        }

        return null
    }

    /**
     * Jumps directly to the system gallery.
     * If a target media URI is provided, opens the gallery's photo/video viewer directly.
     * If no media URI is provided or media view fails, opens the gallery app homepage.
     */
    fun openGallery(context: Context, mediaUri: Uri? = null, isVideo: Boolean = false) {
        val pm = context.packageManager
        val galleryPkg = resolveGalleryPackage(context)

        // Path A: If a specific photo/video URI exists, attempt to open it directly in the gallery
        if (mediaUri != null) {
            val mimeType = if (isVideo) "video/*" else "image/*"

            // 1. Direct view intent targeted at the resolved gallery package (avoids WeChat / system chooser)
            if (galleryPkg != null) {
                val directIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(mediaUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage(galleryPkg)
                }
                if (directIntent.resolveActivity(pm) != null) {
                    runCatching {
                        context.startActivity(directIntent)
                        return
                    }
                }

                // 2. Camera Review action (supported by Xiaomi/Google Camera/MIUI Gallery)
                val reviewIntent = Intent("com.android.camera.action.REVIEW").apply {
                    setDataAndType(mediaUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage(galleryPkg)
                }
                if (reviewIntent.resolveActivity(pm) != null) {
                    runCatching {
                        context.startActivity(reviewIntent)
                        return
                    }
                }
            }

            // 3. Fallback: Unrestricted ACTION_VIEW
            val fallbackViewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(mediaUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (fallbackViewIntent.resolveActivity(pm) != null) {
                val launched = runCatching {
                    context.startActivity(fallbackViewIntent)
                    true
                }.getOrDefault(false)
                if (launched) return
            }
        }

        // Path B: Open Gallery Home (when no media exists yet, or media viewing could not be opened)
        // 1. Standard CATEGORY_APP_GALLERY
        val mainGalleryIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (mainGalleryIntent.resolveActivity(pm) != null) {
            runCatching {
                context.startActivity(mainGalleryIntent)
                return
            }
        }

        // 2. Launch intent for resolved gallery package
        if (galleryPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(galleryPkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                runCatching {
                    context.startActivity(launchIntent)
                    return
                }
            }
        }

        // 3. Try each known OEM gallery package launcher
        for (pkg in KNOWN_GALLERY_PACKAGES) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                val launched = runCatching {
                    context.startActivity(launchIntent)
                    true
                }.getOrDefault(false)
                if (launched) return
            }
        }

        Toast.makeText(context, "未找到可用系统相册应用", Toast.LENGTH_SHORT).show()
    }
}
