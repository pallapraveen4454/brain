package com.example

import com.example.data.GeminiQuizService
import com.example.ui.navigation.ScreenRoute
import com.example.viewmodel.AiQuickAnswerViewModel
import com.example.viewmodel.HomeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiQuickAnswerTest {

    @Test
    fun testQuickModesOptionsContainAiQuickAnswerAndNoDuplicateDaily() {
        val homeViewModel = HomeViewModel()
        val quickOptions = homeViewModel.uiState.value.quickPlayOptions

        assertEquals("Quick modes must have 3 options", 3, quickOptions.size)

        val quickOptionIds = quickOptions.map { it.id }
        assertTrue("Must contain 'quick'", quickOptionIds.contains("quick"))
        assertTrue("Must contain 'ai_quick_answer'", quickOptionIds.contains("ai_quick_answer"))
        assertTrue("Must contain 'ai_custom'", quickOptionIds.contains("ai_custom"))
        assertFalse("Must NOT contain duplicate 'daily' in quick play options", quickOptionIds.contains("daily"))

        val aiAnswerOption = quickOptions.first { it.id == "ai_quick_answer" }
        assertEquals("AI Quick Answer", aiAnswerOption.title)
        assertEquals("Ask anything. Get instant answers.", aiAnswerOption.subtitle)
        assertEquals("AI", aiAnswerOption.badgeText)
    }

    @Test
    fun testAiQuickAnswerScreenRoute() {
        assertEquals("ai_quick_answer_screen", ScreenRoute.AiQuickAnswer.route)
    }

    @Test
    fun testAiQuickAnswerViewModelInitialStateAndInput() {
        val viewModel = AiQuickAnswerViewModel()
        val initialUiState = viewModel.uiState.value

        assertTrue(initialUiState.messages.isEmpty())
        assertEquals("", initialUiState.inputText)
        assertFalse(initialUiState.isLoading)

        viewModel.onInputTextChanged("Who invented the telephone?")
        assertEquals("Who invented the telephone?", viewModel.uiState.value.inputText)

        viewModel.clearConversation()
        assertEquals("", viewModel.uiState.value.inputText)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun testGeminiQuickAnswerEmptyPromptValidation() = runBlocking {
        val service = GeminiQuizService()
        val result = service.generateQuickAnswer("   ")
        assertTrue("Empty prompt must return failure", result.isFailure)
    }
}
