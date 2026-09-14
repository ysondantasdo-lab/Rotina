package br.com.ysondantas.rotina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import br.com.ysondantas.rotina.ui.AppNavigation
import br.com.ysondantas.rotina.ui.theme.RotinaCriancasTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Persistência offline: já vem ativada por padrão no Firestore,
        // esta chamada só deixa isso explícito.
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = db.firestoreSettings.toBuilder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()

        setContent {
            RotinaCriancasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
