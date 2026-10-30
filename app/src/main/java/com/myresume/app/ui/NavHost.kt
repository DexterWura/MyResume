package com.myresume.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myresume.app.AppContainer
import com.myresume.app.ui.ai.AiAssistantScreen
import com.myresume.app.ui.editor.EditorScreen
import com.myresume.app.ui.editor.EditorViewModel
import com.myresume.app.ui.home.HomeScreen
import com.myresume.app.ui.home.HomeViewModel
import com.myresume.app.ui.paywall.PaywallScreen
import com.myresume.app.ui.preview.PreviewScreen
import com.myresume.app.ui.settings.SettingsScreen
import com.myresume.app.ui.templates.TemplateGalleryScreen

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{resumeId}"
    const val TEMPLATES = "templates/{resumeId}"
    const val PREVIEW = "preview/{resumeId}"
    const val PAYWALL = "paywall?product={product}"
    const val AI = "ai/{resumeId}"
    const val SETTINGS = "settings"

    fun editor(resumeId: String) = "editor/$resumeId"
    fun templates(resumeId: String) = "templates/$resumeId"
    fun preview(resumeId: String) = "preview/$resumeId"
    fun paywall(product: String = "") = "paywall?product=$product"
    fun ai(resumeId: String) = "ai/$resumeId"
}

@Composable
fun MyResumeNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember(container) { AppViewModelFactory(container) }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = vm,
                onOpenResume = { id -> navController.navigate(Routes.editor(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("resumeId") { type = NavType.StringType })
        ) { entry ->
            val resumeId = entry.arguments?.getString("resumeId").orEmpty()
            val vm: EditorViewModel = viewModel(
                key = resumeId,
                factory = EditorViewModel.factory(container, resumeId)
            )
            EditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onTemplates = { navController.navigate(Routes.templates(resumeId)) },
                onPreview = { navController.navigate(Routes.preview(resumeId)) },
                onAi = { navController.navigate(Routes.ai(resumeId)) },
                onPaywall = { product -> navController.navigate(Routes.paywall(product)) }
            )
        }
        composable(
            route = Routes.TEMPLATES,
            arguments = listOf(navArgument("resumeId") { type = NavType.StringType })
        ) { entry ->
            val resumeId = entry.arguments?.getString("resumeId").orEmpty()
            TemplateGalleryScreen(
                container = container,
                resumeId = resumeId,
                onBack = { navController.popBackStack() },
                onPaywall = { navController.navigate(Routes.paywall("premium_templates")) }
            )
        }
        composable(
            route = Routes.PREVIEW,
            arguments = listOf(navArgument("resumeId") { type = NavType.StringType })
        ) { entry ->
            val resumeId = entry.arguments?.getString("resumeId").orEmpty()
            PreviewScreen(
                container = container,
                resumeId = resumeId,
                onBack = { navController.popBackStack() },
                onPaywall = { navController.navigate(Routes.paywall("premium_templates")) }
            )
        }
        composable(
            route = Routes.PAYWALL,
            arguments = listOf(
                navArgument("product") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { entry ->
            val product = entry.arguments?.getString("product").orEmpty()
            PaywallScreen(
                container = container,
                highlightProduct = product,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.AI,
            arguments = listOf(navArgument("resumeId") { type = NavType.StringType })
        ) { entry ->
            val resumeId = entry.arguments?.getString("resumeId").orEmpty()
            AiAssistantScreen(
                container = container,
                resumeId = resumeId,
                onBack = { navController.popBackStack() },
                onPaywall = { navController.navigate(Routes.paywall("ai_assistant")) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onPaywall = { navController.navigate(Routes.paywall()) }
            )
        }
    }
}
