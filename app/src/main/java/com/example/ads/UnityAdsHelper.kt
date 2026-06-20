package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

object UnityAdsHelper {
    private const val GAME_ID = "800076821"
    private const val BANNER_PLACEMENT = "Banner_Android"
    
    fun initialize(context: Context) {
        UnityAds.initialize(context, GAME_ID, false, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d("UnityAds", "Initialization Complete")
            }
            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                Log.e("UnityAds", "Initialization Failed: $error - $message")
            }
        })
    }
}

@Composable
fun UnityBannerAd(modifier: Modifier = Modifier, activity: Activity) {
    AndroidView(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        factory = { context ->
            val bannerView = BannerView(activity, "Banner_Android", UnityBannerSize(320, 50))
            bannerView.load()
            
            FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addView(bannerView)
            }
        }
    )
}
