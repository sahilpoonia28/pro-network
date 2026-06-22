package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.Message
import com.example.data.NetworkUser
import com.example.data.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(viewModel: NetworkViewModel) {
    val posts by viewModel.posts.collectAsState()
    var showCreatePost by remember { mutableStateOf(false) }
    var postText by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreatePost = true }, modifier = Modifier.testTag("create_post_fab")) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(posts) { post ->
                PostCard(post)
            }
        }
    }

    if (showCreatePost) {
        AlertDialog(
            onDismissRequest = { showCreatePost = false },
            title = { Text("Create Post") },
            text = {
                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp).testTag("post_input"),
                    placeholder = { Text("What do you want to talk about?") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (postText.isNotBlank()) {
                            viewModel.createPost(postText)
                            postText = ""
                            showCreatePost = false
                        }
                    },
                    modifier = Modifier.testTag("submit_post_btn")
                ) {
                    Text("Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePost = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PostCard(post: Post) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.authorAvatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = post.authorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = post.authorTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(post.timestamp))
                    Text(text = date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = post.content, style = MaterialTheme.typography.bodyMedium)
            
            post.postImageUrl?.let { img ->
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = img,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                PostAction(icon = Icons.Default.ThumbUp, label = "Like (${post.likesCount})")
                PostAction(icon = Icons.Default.Comment, label = "Comment (${post.commentsCount})")
            }
        }
    }
}

@Composable
fun PostAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    TextButton(onClick = { /*TODO*/ }) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(viewModel: NetworkViewModel) {
    val connections by viewModel.connections.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        val requests = suggestions.filter { it.hasPendingRequest }
        if (requests.isNotEmpty()) {
            item {
                Text(
                    text = "Invitations",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(requests) { req ->
                ConnectionRequestCard(req, onAccept = { viewModel.connect(req) }, onIgnore = { viewModel.ignore(req) })
                HorizontalDivider()
            }
        }

        item {
            Text(
                text = "People you may know",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(suggestions.filter { !it.hasPendingRequest }) { user ->
            UserSuggestionCard(user, onConnect = { viewModel.connect(user) })
            HorizontalDivider()
        }
    }
}

@Composable
fun ConnectionRequestCard(user: NetworkUser, onAccept: () -> Unit, onIgnore: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(56.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(text = user.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text(text = "${user.mutualConnections} mutual connections", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
            Row {
                IconButton(onClick = onIgnore, modifier = Modifier.testTag("ignore_${user.id}")) {
                    Icon(Icons.Default.Close, contentDescription = "Ignore", tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                }
                IconButton(onClick = onAccept, modifier = Modifier.testTag("accept_${user.id}")) {
                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
fun UserSuggestionCard(user: NetworkUser, onConnect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(56.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = user.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onConnect,
                modifier = Modifier.testTag("connect_${user.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape
            ) {
                Text("Connect")
            }
        }
    }
}

@Composable
fun MessagesScreen(viewModel: NetworkViewModel, onChatClick: (String, String, String) -> Unit) {
    val recentChats by viewModel.recentChats.collectAsState()
    
    if (recentChats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No messages yet. Connect with someone to start chatting!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(recentChats) { msg ->
                MessagePreviewCard(msg, onClick = { onChatClick(msg.counterpartId, msg.counterpartName, msg.counterpartAvatarUrl) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun MessagePreviewCard(msg: Message, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = msg.counterpartAvatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(56.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = msg.counterpartName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(msg.timestamp))
                    Text(text = date, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val prefix = if (msg.senderId == "me") "You: " else ""
                Text(text = prefix + msg.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: NetworkViewModel,
    counterpartId: String,
    counterpartName: String,
    counterpartAvatarUrl: String,
    onBack: () -> Unit
) {
    val messages by viewModel.getChatMessages(counterpartId).collectAsState()
    var text by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(counterpartName) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                     // Normally back arrow icon
                     Text("← ", style = MaterialTheme.typography.titleLarge)
                }
            }
        )
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), reverseLayout = true) {
            items(messages.reversed()) { msg ->
                val isMe = msg.senderId == "me"
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(12.dp),
                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write a message...") },
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        viewModel.sendMessage(counterpartId, counterpartName, counterpartAvatarUrl, text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .testTag("send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = "https://i.pravatar.cc/150?u=me",
            contentDescription = "My Avatar",
            modifier = Modifier.size(120.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Me (You)", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Android Developer | Looking for opportunities", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Passionate Android developer with experience in building clean, scalable apps using Jetpack Compose and modern Kotlin practices.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
