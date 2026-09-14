package br.com.ysondantas.rotina.ui

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.ysondantas.rotina.data.DiaSemana
import br.com.ysondantas.rotina.data.RotinaRepository

@Composable
fun AppNavigation(
    rotinaRepo: RotinaRepository = RotinaRepository()
) {
    val navController = rememberNavController()
    
    // O ID da família agora é fixo e direto, sem precisar carregar login
    val familyId = "familia_dantas_2026"

    // Guardamos o nome da criança selecionada em memória simples para as próximas telas
    var criancaSelecionadaId by remember { mutableStateOf<String?>(null) }
    var criancaSelecionadaNome by remember { mutableStateOf<String?>(null) }

    // O app inicia direto na HomeScreen (Tela Inicial)
    NavHost(navController = navController, startDestination = "home") {
        
        // 1. Tela Inicial: Lista as crianças da família
        composable("home") {
            HomeScreen(familyId = familyId, repo = rotinaRepo) { crianca ->
                criancaSelecionadaId = crianca.id
                criancaSelecionadaNome = crianca.nome
                navController.navigate("semana")
            }
        }
        
        // 2. Tela dos Dias da Semana
        composable("semana") {
            val crianca = br.com.ysondantas.rotina.data.Crianca(
                id = criancaSelecionadaId ?: "", nome = criancaSelecionadaNome ?: ""
            )
            WeekScreen(
                crianca = crianca,
                aoVoltar = { navController.popBackStack() },
                aoAbrirDia = { dia -> navController.navigate("dia/${dia.name}") }
            )
        }
        
        // 3. Tela do Dia Específico (Lista os eventos e permite adicionar/excluir)
        composable(
            route = "dia/{diaNome}",
            arguments = listOf(navArgument("diaNome") { type = NavType.StringType })
        ) { backStackEntry ->
            val diaNome = backStackEntry.arguments?.getString("diaNome") ?: DiaSemana.SEGUNDA.name
            val dia = DiaSemana.valueOf(diaNome)
            val crianca = br.com.ysondantas.rotina.data.Crianca(
                id = criancaSelecionadaId ?: "", nome = criancaSelecionadaNome ?: ""
            )
            DayScreen(
                familyId = familyId,
                crianca = crianca,
                dia = dia,
                repo = rotinaRepo,
                aoVoltar = { navController.popBackStack() }
            )
        }
    }
}
