package com.mobitechs.parcelwala.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.R
import kotlinx.coroutines.delay

/**
 * How long the splash stays up before navigating. Was 2000 ms of dead time on
 * every cold start; see the note in [SplashScreen].
 */
//private const val SPLASH_MIN_VISIBLE_MS = 450L
private const val SPLASH_MIN_VISIBLE_MS = 2000L

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    isLoggedIn: Boolean
) {
    LaunchedEffect(Unit) {
        // PERFORMANCE — this was a flat `delay(2000)`.
        //
        // The splash does no work: the login check is a synchronous
        // SharedPreferences read that has already happened by the time this
        // composes. The two seconds were pure, unconditional dead time added to
        // every single cold start, and they were the largest component of
        // "the app is slow to open".
        //
        // Kept as a brief brand beat so the logo does not flash past, but short
        // enough that the app feels immediate. Set to 0 to remove it entirely.
        delay(SPLASH_MIN_VISIBLE_MS)
        if (isLoggedIn) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.pw_splash),
                contentDescription = stringResource(R.string.content_desc_app_logo),
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )

//            Spacer(modifier = Modifier.height(16.dp))
//
//            // App Name
//            Text(
//                text = stringResource(R.string.label_app_name_display),
//                fontSize = 32.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.White
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Tagline
//            Text(
//                text = stringResource(R.string.label_tagline),
//                fontSize = 16.sp,
//                color = Color.White.copy(alpha = 0.8f)
//            )
        }
    }
}