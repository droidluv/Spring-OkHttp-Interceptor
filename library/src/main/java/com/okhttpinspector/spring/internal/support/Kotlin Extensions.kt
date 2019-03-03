package com.okhttpinspector.spring.internal.support

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.okhttpinspector.spring.R


fun Activity.changeStatusBarColor(
        color: Int,
        invertStatusBarIconsColor: Boolean
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            window.decorView.systemUiVisibility =
                    if (invertStatusBarIconsColor) window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        window.statusBarColor = color
    }
}


fun String.parseColor(defaultColor: Int = Color.TRANSPARENT): Int {
    return try {
        Color.parseColor(this)
    } catch (ex: IllegalArgumentException) {
        defaultColor
    }
}

@Suppress("DEPRECATION")
fun String.parseHtml(): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(this)
    }
}


infix fun Context.getColorResource(colorResource: Int): Int {
    return try {
        ContextCompat.getColor(this, colorResource)
    } catch (ex: Resources.NotFoundException) {
        ContextCompat.getColor(this, R.color.spring_colorPrimary)
    }
}