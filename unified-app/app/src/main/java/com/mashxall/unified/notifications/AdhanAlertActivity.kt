package com.mashxall.unified.notifications

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mashxall.unified.AppLanguage

class AdhanAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        val prefs = getSharedPreferences("mashxall_preferences", MODE_PRIVATE)
        val language = AppLanguage.from(prefs.getString("language", "ckb"))
        val prayerLabel = intent.getStringExtra(AdhanPlaybackService.EXTRA_PRAYER_LABEL).orEmpty().ifBlank {
            when (language) {
                AppLanguage.EN -> "Prayer time"
                AppLanguage.AR -> "وقت الصلاة"
                AppLanguage.CKB -> "کاتی نوێژ"
            }
        }
        val muezzinName = intent.getStringExtra(AdhanPlaybackService.EXTRA_ADHAN_NAME).orEmpty()
        setContent {
            MaterialTheme {
                AdhanAlertContent(
                    language = language,
                    prayerLabel = prayerLabel,
                    muezzinName = muezzinName,
                    onStop = {
                        AdhanPlaybackService.stopPlayback(this@AdhanAlertActivity)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun AdhanAlertContent(
    language: AppLanguage,
    prayerLabel: String,
    muezzinName: String,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    val direction = if (language == AppLanguage.EN) LayoutDirection.Ltr else LayoutDirection.Rtl
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B211B)) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF123E31), Color(0xFF071B16), Color(0xFF071510)))
                ).padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(104.dp).background(Color(0x22D5B45A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Color(0xFFD5B45A), modifier = Modifier.size(54.dp))
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("مەشخەڵ", color = Color(0xFFD5B45A), fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(18.dp))
                    Text(prayerLabel, modifier = Modifier.fillMaxWidth(), color = Color.White, fontSize = 35.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "The time for prayer has arrived"
                            AppLanguage.AR -> "حان الآن وقت الصلاة"
                            AppLanguage.CKB -> "ئێستا کاتی نوێژە"
                        },
                        color = Color(0xFFC3D5CC), fontSize = 15.sp, textAlign = TextAlign.Center
                    )
                    if (muezzinName.isNotBlank()) {
                        Spacer(Modifier.height(28.dp))
                        Text(
                            when (language) {
                                AppLanguage.EN -> "MUEZZIN"
                                AppLanguage.AR -> "المؤذن المختار"
                                AppLanguage.CKB -> "موئەزینی هەڵبژێردراو"
                            },
                            color = Color(0xFFD5B45A), fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(muezzinName, modifier = Modifier.fillMaxWidth(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(42.dp))
                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD5B45A), contentColor = Color(0xFF082019))
                    ) {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Stop Adhan"
                                AppLanguage.AR -> "إيقاف الأذان"
                                AppLanguage.CKB -> "وەستاندنی بانگ"
                            },
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Dismiss screen"
                                AppLanguage.AR -> "إغلاق الشاشة"
                                AppLanguage.CKB -> "داخستنی شاشە"
                            },
                            color = Color(0xFFC3D5CC), fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}