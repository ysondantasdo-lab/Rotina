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
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Persistência offline explícita para o Firestore guardar dados sem internet
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()

        // 2. Inicializa o Firebase Auth e faz o login anônimo em segundo plano
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener { authResult ->
                    Log.d("Firebase", "Login anônimo efetuado! UID: ${authResult.user?.uid}")
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase", "Falha ao realizar login anônimo", exception)
                }
        } else {
            Log.d("Firebase", "Usuário já autenticado: ${auth.currentUser?.uid}")
        }



            
        setContent {
            RotinaCriancasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
