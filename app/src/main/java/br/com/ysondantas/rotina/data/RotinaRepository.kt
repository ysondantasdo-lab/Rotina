package br.com.ysondantas.rotina.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RotinaRepository {
    private val db = FirebaseFirestore.getInstance()
    
    // ID fixo para ligar o seu celular ao do seu cônjuge automaticamente
    private val FAMILIA_ID = "familia_dantas_2026"

    // 1. Escutar alterações em tempo real (O outro celular vê na hora)
    fun escutarEventosDoDia(criancaId: String, diaSemana: String): Flow<List<Evento>> = callbackFlow {
        val listener = db.collection("families")
            .document(FAMILIA_ID)
            .collection("criancas")
            .document(criancaId)
            .collection("dias")
            .document(diaSemana.lowercase())
            .collection("eventos")
            .orderBy("horario")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val eventos = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.id
                    val horario = doc.getString("horario") ?: ""
                    val descricao = doc.getString("descricao") ?: ""
                    Evento(id = id, horario = horario, descricao = descricao)
                } ?: emptyList()
                
                trySend(eventos)
            }
        
        awaitClose { listener.remove() }
    }

    // 2. Adicionar um novo evento à rotina
    suspend fun adicionarEvento(criancaId: String, diaSemana: String, horario: String, descricao: String) {
        val dados = hashMapOf(
            "horario" to horario,
            "descricao" to descricao
        )
        
        db.collection("families")
            .document(FAMILIA_ID)
            .collection("criancas")
            .document(criancaId)
            .collection("dias")
            .document(diaSemana.lowercase())
            .collection("eventos")
            .add(dados)
            .await()
    }

    // 3. Excluir um evento da rotina
    suspend fun excluirEvento(criancaId: String, diaSemana: String, eventoId: String) {
        db.collection("families")
            .document(FAMILIA_ID)
            .collection("criancas")
            .document(criancaId)
            .collection("dias")
            .document(diaSemana.lowercase())
            .collection("eventos")
            .document(eventoId)
            .delete()
            .await()
    }
        // 4. Escutar a lista de crianças em tempo real
    fun escutarCriancas(): Flow<List<Crianca>> = callbackFlow {
        val listener = db.collection("families")
            .document(FAMILIA_ID)
            .collection("criancas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val criancas = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.id
                    val nome = doc.getString("nome") ?: ""
                    Crianca(id = id, nome = nome)
                } ?: emptyList()
                
                trySend(criancas)
            }
        
        awaitClose { listener.remove() }
    }

    // 5. Cadastrar uma nova criança no banco compartilhado
    suspend fun adicionarCrianca(nome: String) {
        val dados = hashMapOf("nome" to nome)
        db.collection("families")
            .document(FAMILIA_ID)
            .collection("criancas")
            .add(dados)
            .await()
    }

}
