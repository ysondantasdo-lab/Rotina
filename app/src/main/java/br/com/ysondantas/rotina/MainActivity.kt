package br.com.ysondantas.rotina

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import br.com.ysondantas.rotina.ui.AppNavigation
import br.com.ysondantas.rotina.ui.theme.Theme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import androidx.compose.foundation.layout.fillMaxSize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Persistência offline explícita para o Firestore guardar dados sem internet
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = db.firestoreSettings.toBuilder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()

        setContent {
            Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
