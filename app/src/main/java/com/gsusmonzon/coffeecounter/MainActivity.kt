package com.gsusmonzon.coffeecounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gsusmonzon.coffeecounter.ui.CoffeeCounterApp
import com.gsusmonzon.coffeecounter.ui.theme.CoffeeCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoffeeCounterTheme {
                CoffeeCounterApp()
            }
        }
    }
}
