package br.com.ysondantas.rotina.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Todas as leituras usam addSnapshotListener (tempo real): quando um pai
 * altera algo, o Firestore empurra a mudança para o app do outro pai
 * automaticamente — sem precisar de "puxar para atualizar".
 * O SDK do Firestore já guarda cache local, então funciona offline e
 * sincroniza sozinho quando a internet volta.
 */
class RotinaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun criancasRef(familyId: String) =
        db.collection("families").document(familyId).collection("criancas")

    private fun eventosRef(familyId: String, criancaId: String, dia: String) =
        criancasRef(familyId).document(criancaId)
            .collection("dias").document(dia).collection("eventos")

    fun criancasFlow(familyId: String): Flow<List<Crianca>> = callbackFlow {
        val registration = criancasRef(familyId)
            .orderBy("nome")
            .addSnapshotListener { snap, _ ->
                val lista = snap?.documents?.map { doc ->
                    Crianca(id = doc.id, nome = doc.getString("nome") ?: "")
                } ?: emptyList()
                trySend(lista)
            }
        awaitClose { registration.remove() }
    }

    suspend fun adicionarCrianca(familyId: String, nome: String) {
        criancasRef(familyId).add(mapOf("nome" to nome)).await()
    }

    fun eventosFlow(familyId: String, criancaId: String, dia: String): Flow<List<Evento>> =
        callbackFlow {
            val registration = eventosRef(familyId, criancaId, dia)
                .orderBy("horario", Query.Direction.ASCENDING)
                .addSnapshotListener { snap, _ ->
                    val lista = snap?.documents?.map { doc ->
                        Evento(
                            id = doc.id,
                            horario = doc.getString("horario") ?: "",
                            descricao = doc.getString("descricao") ?: ""
                        )
                    } ?: emptyList()
                    trySend(lista)
                }
            awaitClose { registration.remove() }
        }

    suspend fun adicionarEvento(familyId: String, criancaId: String, dia: String, horario: String, descricao: String) {
        eventosRef(familyId, criancaId, dia).add(
            mapOf("horario" to horario, "descricao" to descricao)
        ).await()
    }

    suspend fun editarEvento(familyId: String, criancaId: String, dia: String, eventoId: String, horario: String, descricao: String) {
        eventosRef(familyId, criancaId, dia).document(eventoId).set(
            mapOf("horario" to horario, "descricao" to descricao)
        ).await()
    }

    suspend fun excluirEvento(familyId: String, criancaId: String, dia: String, eventoId: String) {
        eventosRef(familyId, criancaId, dia).document(eventoId).delete().await()
    }
}
