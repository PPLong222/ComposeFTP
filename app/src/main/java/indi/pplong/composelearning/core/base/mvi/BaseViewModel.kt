package indi.pplong.composelearning.core.base.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Description:
 * @author PPLong
 * @date 9/29/24 9:47 PM
 */
abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiEffect> : ViewModel() {
    private val initialState: S by lazy {
        initialState()
    }

    protected abstract fun initialState(): S

    private val _uiState: MutableStateFlow<S> by lazy { MutableStateFlow(initialState) }

    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _uiIntents: MutableSharedFlow<I> = MutableSharedFlow()

    private val _uiEffect = Channel<E>(Channel.BUFFERED)

    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        subscribeIntents()
    }

    private fun subscribeIntents() {
        viewModelScope.launch {
            _uiIntents.collect {
                handleIntent(it)
            }
        }
    }

    fun sendIntent(intent: I) {
        viewModelScope.launch {
            _uiIntents.emit(intent)
        }
    }

    protected fun setState(reduce: S.() -> S) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.reduce()
        }

    }

    abstract suspend fun handleIntent(intent: I)

    protected fun sendEffect(builder: () -> E) {
        viewModelScope.launch {
            _uiEffect.send(builder())
        }
    }

    protected fun launchOnIO(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
        }
    }
}