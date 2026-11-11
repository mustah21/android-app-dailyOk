package com.example.dailyok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.dailyok.ui.checkin.CheckInHost
import com.example.dailyok.ui.navigation.AppNavGraph
import com.example.dailyok.ui.theme.DailyOkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DailyOkApp
        setContent {
//            DailyOkTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    AppNavGraph(app)
//                }
//            }

//            TEST> this is for testing purpose, we remove when app is final
            DailyOkTheme {
                CheckInHost(contactNames = listOf("Anna", "Jussi")) {
                    AppNavGraph(app)   // your normal screen
                }
            }
        }
    }
}
