package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiQuizService
import com.example.utils.NetworkConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val canRetry: Boolean = false,
    val failedQuestion: String? = null
)

data class AiQuickAnswerUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOnline: Boolean = true
)

class AiQuickAnswerViewModel(
    private val geminiService: GeminiQuizService = GeminiQuizService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiQuickAnswerUiState())
    val uiState: StateFlow<AiQuickAnswerUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearConversation() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                inputText = "",
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun askSuggestedQuestion(question: String, context: Context? = null) {
        sendMessage(explicitQuestion = question, context = context)
    }

    fun retryQuestion(failedQuestion: String, context: Context? = null) {
        // Remove trailing error message before retrying
        _uiState.update { current ->
            val filtered = current.messages.filterNot { it.isError && it.failedQuestion == failedQuestion }
            current.copy(messages = filtered, errorMessage = null)
        }
        sendMessage(explicitQuestion = failedQuestion, context = context)
    }

    fun sendMessage(explicitQuestion: String? = null, context: Context? = null) {
        val question = (explicitQuestion ?: _uiState.value.inputText).trim()
        if (question.isBlank() || _uiState.value.isLoading) return

        // Check network connectivity if context provided
        if (context != null) {
            val isOnline = NetworkConnectivityObserver.getInstance(context).isOnline.value
            if (!isOnline) {
                val errorMsg = "Internet connection is required to ask questions. Please check your network and try again."
                _uiState.update { current ->
                    current.copy(
                        errorMessage = errorMsg,
                        messages = current.messages + ChatMessage(
                            text = question,
                            isUser = true
                        ) + ChatMessage(
                            text = errorMsg,
                            isUser = false,
                            isError = true,
                            canRetry = true,
                            failedQuestion = question
                        )
                    )
                }
                return
            }
        }

        // Build recent conversation turns for context management (up to 4 previous valid turns)
        val currentMessages = _uiState.value.messages.filterNot { it.isError }
        val recentHistory = mutableListOf<Pair<String, String>>()
        var lastUserMsg: String? = null

        for (msg in currentMessages) {
            if (msg.isUser) {
                lastUserMsg = msg.text
            } else if (lastUserMsg != null) {
                recentHistory.add(Pair(lastUserMsg, msg.text))
                lastUserMsg = null
            }
        }

        val userMessage = ChatMessage(text = question, isUser = true)

        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                inputText = if (explicitQuestion == null) "" else current.inputText,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = geminiService.generateQuickAnswer(question, recentHistory)
            result.fold(
                onSuccess = { answer ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            messages = current.messages + ChatMessage(
                                text = answer,
                                isUser = false
                            ),
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    val friendlyError = when {
                        error.message?.contains("API key", ignoreCase = true) == true ->
                            "AI service is currently unavailable. Please verify API configuration."
                        error.message?.contains("network", ignoreCase = true) == true ||
                                error.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                            "Network connection lost. Please check your internet connection."
                        error.message?.contains("timeout", ignoreCase = true) == true ->
                            "The request timed out. Please try again."
                        else ->
                            "Couldn't get an answer right now. Please try again."
                    }

                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            messages = current.messages + ChatMessage(
                                text = friendlyError,
                                isUser = false,
                                isError = true,
                                canRetry = true,
                                failedQuestion = question
                            ),
                            errorMessage = friendlyError
                        )
                    }
                }
            )
        }
    }
}
