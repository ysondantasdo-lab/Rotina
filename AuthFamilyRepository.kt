package br.com.ysondantas.rotina

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Cuida do login (Firebase Authentication) e do vínculo entre os dois pais
 * (Firebase Authentication sozinho não sabe "quem é a família de quem",
 * então guardamos um documento em /users/{uid} com o familyId, e os dois
 * pais compartilham o mesmo familyId).
 */
class AuthFamilyRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val uidAtual: String? get() = auth.currentUser?.uid

    suspend fun cadastrar(email: String, senha: String) {
        auth.createUserWithEmailAndPassword(email, senha).await()
    }

    suspend fun entrar(email: String, senha: String) {
        auth.signInWithEmailAndPassword(email, senha).await()
    }

    fun sair() = auth.signOut()

    /** Cria uma família nova e retorna o código de convite para o outro pai digitar. */
    suspend fun criarFamilia(): String {
        val uid = uidAtual ?: error("Usuário não autenticado")
        val codigo = (1..6).map { Random.nextInt(0, 10) }.joinToString("")
        val familyRef = db.collection("families").document(codigo)
        familyRef.set(mapOf("membros" to listOf(uid))).await()
        db.collection("users").document(uid).set(mapOf("familyId" to codigo)).await()
        return codigo
    }

    /** O segundo pai usa o código gerado pelo primeiro para entrar na mesma família. */
    suspend fun entrarNaFamilia(codigo: String) {
        val uid = uidAtual ?: error("Usuário não autenticado")
        val familyRef = db.collection("families").document(codigo)
        val doc = familyRef.get().await()
        if (!doc.exists()) error("Código de família não encontrado")
        familyRef.update("membros", com.google.firebase.firestore.FieldValue.arrayUnion(uid)).await()
        db.collection("users").document(uid).set(mapOf("familyId" to codigo)).await()
    }

    /** Observa em tempo real qual é o familyId do usuário logado (null se ainda não tem família). */
    fun familyIdFlow(): Flow<String?> = callbackFlow {
        val uid = uidAtual
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.getString("familyId"))
            }
        awaitClose { registration.remove() }
    }
}
