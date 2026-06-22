package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val authorTitle: String,
    val authorAvatarUrl: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val postImageUrl: String? = null
)

@Entity(tableName = "network_users")
data class NetworkUser(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val title: String,
    val avatarUrl: String,
    val isConnected: Boolean = false,
    val hasPendingRequest: Boolean = false,
    val mutualConnections: Int = 0
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val counterpartId: String,
    val counterpartName: String,
    val counterpartAvatarUrl: String,
    val senderId: String, // "me" or counterpartId
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface NetworkDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Query("SELECT * FROM network_users WHERE isConnected = 1")
    fun getConnections(): Flow<List<NetworkUser>>

    @Query("SELECT * FROM network_users WHERE isConnected = 0")
    fun getSuggestions(): Flow<List<NetworkUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: NetworkUser)

    @Update
    suspend fun updateUser(user: NetworkUser)

    @Query("SELECT * FROM messages WHERE counterpartId = :counterpartId ORDER BY timestamp ASC")
    fun getMessagesForChat(counterpartId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages GROUP BY counterpartId ORDER BY timestamp DESC")
    fun getRecentChats(): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
}

@Database(entities = [Post::class, NetworkUser::class, Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun networkDao(): NetworkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pronetwork_db"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.networkDao())
                }
            }
        }

        suspend fun populateInitialData(dao: NetworkDao) {
            val user1 = NetworkUser(id = "u1", name = "Jane Doe", title = "Senior Software Engineer @ TechCorp", avatarUrl = "https://i.pravatar.cc/150?u=jane", mutualConnections = 12, isConnected = false)
            val user2 = NetworkUser(id = "u2", name = "John Smith", title = "Product Manager at Innovate", avatarUrl = "https://i.pravatar.cc/150?u=john", mutualConnections = 5, hasPendingRequest = true)
            val user3 = NetworkUser(id = "u3", name = "Alice Johnson", title = "Recruiter | Hiring Engineers", avatarUrl = "https://i.pravatar.cc/150?u=alice", mutualConnections = 8, isConnected = true)
            dao.insertUser(user1)
            dao.insertUser(user2)
            dao.insertUser(user3)

            dao.insertPost(Post(
                authorName = "Alice Johnson",
                authorTitle = "Recruiter | Hiring Engineers",
                authorAvatarUrl = "https://i.pravatar.cc/150?u=alice",
                content = "We are hiring! Looking for talented Android developers to join our fast-paced remote team. 🚀 #android #hiring #jobs",
                likesCount = 42,
                commentsCount = 5,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2 // 2 hours ago
            ))

            dao.insertPost(Post(
                authorName = "Mark Zuckerberg",
                authorTitle = "CEO @ Meta",
                authorAvatarUrl = "https://i.pravatar.cc/150?u=mark",
                content = "Just announced the new features for our platform. The future is connected. What are your thoughts on the latest update?",
                likesCount = 1045,
                commentsCount = 120,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5
            ))
            
            // Initial chats
            dao.insertMessage(Message(
                counterpartId = "u3",
                counterpartName = "Alice Johnson",
                counterpartAvatarUrl = "https://i.pravatar.cc/150?u=alice",
                senderId = "u3",
                text = "Hi there! I saw your profile and thought your background is a great fit for our open Android position.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24
            ))
        }
    }
}

class NetworkRepository(private val db: AppDatabase) {
    private val dao = db.networkDao()

    val posts = dao.getAllPosts()
    val connections = dao.getConnections()
    val suggestions = dao.getSuggestions()
    val recentChats = dao.getRecentChats()

    suspend fun createPost(content: String, imgUrl: String? = null) {
        dao.insertPost(Post(
            authorName = "Me (You)",
            authorTitle = "Android Developer | Looking for opportunities",
            authorAvatarUrl = "https://i.pravatar.cc/150?u=me",
            content = content,
            postImageUrl = imgUrl
        ))
    }

    suspend fun connectWithUser(user: NetworkUser) {
        dao.updateUser(user.copy(isConnected = true, hasPendingRequest = false))
    }
    
    suspend fun ignoreRequest(user: NetworkUser) {
        dao.updateUser(user.copy(hasPendingRequest = false))
    }

    fun getMessages(counterpartId: String) = dao.getMessagesForChat(counterpartId)

    suspend fun sendMessage(counterpartId: String, counterpartName: String, counterpartAvatarUrl: String, text: String) {
        dao.insertMessage(Message(
            counterpartId = counterpartId,
            counterpartName = counterpartName,
            counterpartAvatarUrl = counterpartAvatarUrl,
            senderId = "me",
            text = text
        ))
    }
}
