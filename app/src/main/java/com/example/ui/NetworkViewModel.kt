package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.NetworkRepository
import com.example.data.NetworkUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetworkViewModel(private val repository: NetworkRepository) : ViewModel() {
    
    val posts = repository.posts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val connections = repository.connections.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val suggestions = repository.suggestions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recentChats = repository.recentChats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun getChatMessages(counterpartId: String) = repository.getMessages(counterpartId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun createPost(content: String) {
        viewModelScope.launch {
            repository.createPost(content)
        }
    }

    fun connect(user: NetworkUser) {
        viewModelScope.launch {
            repository.connectWithUser(user)
        }
    }

    fun ignore(user: NetworkUser) {
        viewModelScope.launch {
            repository.ignoreRequest(user)
        }
    }

    fun sendMessage(counterpartId: String, counterpartName: String, counterpartAvatarUrl: String, text: String) {
        viewModelScope.launch {
            repository.sendMessage(counterpartId, counterpartName, counterpartAvatarUrl, text)
        }
    }
}

class NetworkViewModelFactory(private val repository: NetworkRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NetworkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NetworkViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
