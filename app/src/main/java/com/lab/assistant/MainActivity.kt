package com.lab.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lab.assistant.ui.AppNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LabTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { AppNav() } } }
    }
}

@Composable
fun LabTheme(c: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = c)
}
