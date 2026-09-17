package br.com.ysondantas.rotina.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RotinaRepository {
    private val db = FirebaseFirestore.getInstance()

    // ID fixo para ligar o seu celular ao do seu cônjuge automaticamente
    private val FAMILIA_ID = "familia_dantas_2026"

    private fun eventosRef(criancaId: String, diaSemana: String) = db.collection("families")
        .document(FAMILIA_ID)
        .collection("criancas")
        .document(criancaId)
        .collection("dias")
        .document(diaSemana.lowercase())
        .collection("eventos")

    // 1. Escutar alterações em tempo real (O outro celular vê na hora)
    fun escutarEventosDoDia(criancaId: String, diaSemana: String): Flow<List<Evento>> = callbackFlow {
        val listener = eventosRef(criancaId, diaSemana)
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
                    val grupoId = doc.getString("grupoId") ?: ""
                    val diasRepeticao = (doc.get("diasRepeticao") as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList()
                    Evento(
                        id = id,
                        horario = horario,
                        descricao = descricao,
                        grupoId = grupoId,
                        diasRepeticao = diasRepeticao
                    )
                } ?: emptyList()

                trySend(eventos)
            }

        awaitClose { listener.remove() }
    }

    // 2. Adicionar um novo evento, repetindo em um ou mais dias da semana
    suspend fun adicionarEvento(criancaId: String, dias: List<String>, horario: String, descricao: String) {
        val ehGrupo = dias.size > 1
        val grupoId = if (ehGrupo) UUID.randomUUID().toString() else ""
        val diasRepeticao = if (ehGrupo) dias else emptyList()

        for (dia in dias) {
            val dados = hashMapOf(
                "horario" to horario,
                "descricao" to descricao,
                "grupoId" to grupoId,
                "diasRepeticao" to diasRepeticao
            )
            eventosRef(criancaId, dia).add(dados).await()
        }
    }

    // 3. Excluir um evento da rotina (apaga só o documento desse dia)
    suspend fun excluirEvento(criancaId: String, diaSemana: String, eventoId: String) {
        eventosRef(criancaId, diaSemana).document(eventoId).delete().await()
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

    // 6. Atualizar um evento aplicando a mudança a TODOS os dias em que ele se repete,
    //    reconciliando o conjunto de dias (remove os que saíram, atualiza os que ficaram,
    //    cria os que entraram). Também é usado quando um evento de 1 dia passa a ter mais dias.
    suspend fun atualizarEventoGrupo(
        criancaId: String,
        diaAtual: String,
        evento: Evento,
        novoHorario: String,
        novaDescricao: String,
        novosDias: List<String>
    ) {
        val grupoIdAntigo = evento.grupoId
        val diasAntigos = if (evento.diasRepeticao.isNotEmpty()) evento.diasRepeticao else listOf(diaAtual)
        val novosDiasEfetivos = if (novosDias.isEmpty()) listOf(diaAtual) else novosDias

        val ehGrupoFinal = novosDiasEfetivos.size > 1
        val grupoIdFinal = if (ehGrupoFinal) {
            grupoIdAntigo.ifBlank { UUID.randomUUID().toString() }
        } else ""
        val diasRepeticaoFinal = if (ehGrupoFinal) novosDiasEfetivos else emptyList()

        val diasRemover = diasAntigos - novosDiasEfetivos.toSet()
        val diasManter = diasAntigos.toSet().intersect(novosDiasEfetivos.toSet())
        val diasAdicionar = novosDiasEfetivos.toSet() - diasAntigos.toSet()

        val dadosAtualizados = hashMapOf(
            "horario" to novoHorario,
            "descricao" to novaDescricao,
            "grupoId" to grupoIdFinal,
            "diasRepeticao" to diasRepeticaoFinal
        )

        // Remove os dias que saíram do grupo
        for (dia in diasRemover) {
            val docId = buscarDocIdDoGrupo(criancaId, dia, grupoIdAntigo) ?: continue
            eventosRef(criancaId, dia).document(docId).delete().await()
        }

        // Atualiza os dias que continuam no grupo
        for (dia in diasManter) {
            val docId = if (dia == diaAtual) evento.id else buscarDocIdDoGrupo(criancaId, dia, grupoIdAntigo)
            if (docId != null) {
                eventosRef(criancaId, dia).document(docId).update(dadosAtualizados as Map<String, Any>).await()
            }
        }

        // Cria os dias novos que entraram no grupo
        for (dia in diasAdicionar) {
            eventosRef(criancaId, dia).add(dadosAtualizados).await()
        }
    }

    // 7. Atualizar um evento SÓ no dia atual, desvinculando-o do grupo de repetição
    //    (os outros dias do grupo deixam de referenciar esse dia).
    suspend fun atualizarEventoApenasEsseDia(
        criancaId: String,
        diaAtual: String,
        evento: Evento,
        novoHorario: String,
        novaDescricao: String
    ) {
        // Atualiza só este dia, virando um evento independente
        val dadosEsteDia = hashMapOf(
            "horario" to novoHorario,
            "descricao" to novaDescricao,
            "grupoId" to "",
            "diasRepeticao" to emptyList<String>()
        )
        eventosRef(criancaId, diaAtual).document(evento.id).update(dadosEsteDia as Map<String, Any>).await()

        // Remove esse dia da lista dos outros dias do grupo
        if (evento.grupoId.isNotBlank()) {
            val diasRestantes = evento.diasRepeticao - diaAtual
            val outrosDias = evento.diasRepeticao.filter { it != diaAtual }

            for (dia in outrosDias) {
                val docId = buscarDocIdDoGrupo(criancaId, dia, evento.grupoId) ?: continue
                if (diasRestantes.size > 1) {
                    eventosRef(criancaId, dia).document(docId)
                        .update(mapOf("diasRepeticao" to diasRestantes)).await()
                } else {
                    // Só sobrou 1 dia no grupo -> desfaz o grupo também nesse último dia
                    eventosRef(criancaId, dia).document(docId)
                        .update(mapOf("grupoId" to "", "diasRepeticao" to emptyList<String>())).await()
                }
            }
        }
    }

    private suspend fun buscarDocIdDoGrupo(criancaId: String, diaSemana: String, grupoId: String): String? {
        if (grupoId.isBlank()) return null
        val snapshot = eventosRef(criancaId, diaSemana)
            .whereEqualTo("grupoId", grupoId)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.id
    }
}