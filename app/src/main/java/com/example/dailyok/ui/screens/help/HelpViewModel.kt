package com.example.dailyok.ui.screens.help

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class HelpViewModel : ViewModel() {

    var helpRequested by mutableStateOf(false)
        private set

    fun requestHelp() {
        helpRequested = true
    }

}