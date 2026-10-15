package com.finsim.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finsim.core.FinsimLog
import com.finsim.persistence.ScenarioStorage

class AndroidScenarioStorage(context: Context) : ScenarioStorage {
    private val prefs = context.getSharedPreferences("finsim_prefs", Context.MODE_PRIVATE)

    override fun getItem(key: String): String? = prefs.getString(key, null)

    override fun setItem(key: String, value: String) {
        check(prefs.edit().putString(key, value).commit()) { "No se pudo guardar en el dispositivo." }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FinsimLog.ui.i { "MainActivity onCreate: Finsim Android iniciada en SDK ${android.os.Build.VERSION.SDK_INT}" }
        val storage = AndroidScenarioStorage(this)
        setContent {
            App(storage = storage)
        }
    }
}
