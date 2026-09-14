package br.com.ysondantas.rotina.data

data class Crianca(
    val id: String = "",
    val nome: String = ""
)

data class Evento(
    val id: String = "",
    val horario: String = "",   // formato "HH:mm", ex: "07:30"
    val descricao: String = ""  // ex: "Escola", "Natação", "Dormir"
)

/**
 * Dias da semana usados como ID de documento no Firestore
 * (chave em minúsculo/sem acento -> label para exibir na tela).
 */
enum class DiaSemana(val chave: String, val label: String) {
    SEGUNDA("segunda", "SEGUNDA"),
    TERCA("terca", "TERÇA"),
    QUARTA("quarta", "QUARTA"),
    QUINTA("quinta", "QUINTA"),
    SEXTA("sexta", "SEXTA"),
    SABADO("sabado", "SÁBADO"),
    DOMINGO("domingo", "DOMINGO")
}
