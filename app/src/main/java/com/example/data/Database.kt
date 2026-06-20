package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "saarthi"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String? = null,
    val isVoice: Boolean = false
)

@Entity(tableName = "memory_items")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "Name", "Goal", "Habit", "Interest", "Preference", "Milestone", "Other"
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val userName: String = "Friend",
    val preferredLanguage: String = "Auto", // "Auto", "English", "Hindi", "Hinglish"
    val currentMood: String = "Normal",
    val themeMode: String = "Auto", // "Auto" (Adaptive), "Light", "Dark"
    val userEmail: String = "",
    val passcodeHash: String = "", // Secure SHA-256 hashed password/PIN
    val isLoggedIn: Boolean = false,
    val subscriptionPlan: String = "Free Tier", // "Free Tier", "Weekly Explorer", "Monthly Companion", "Yearly Zen"
    val subscriptionExpiry: Long = 0L,         // Epoch millisecond expiry timestamp
    val subscriptionStatus: String = "Inactive" // "Active", "Expired", "Inactive"
)

@Entity(tableName = "mood_records")
data class MoodRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mood: String = "Normal",
    val confidence: Int = 5, // 1 to 10
    val clarity: Int = 5, // 1 to 10
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reflection_summaries")
data class ReflectionSummary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalText: String,
    val detectedFeelings: String,
    val keyTakeaways: String,
    val actionableSteps: String,
    val mainMoodEmoji: String = "🌱",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryItem)

    @Delete
    suspend fun deleteMemory(memory: MemoryItem)

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteMemoryById(id: Int)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemories()
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfile)
}

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_records ORDER BY timestamp ASC")
    fun getAllMoodRecords(): Flow<List<MoodRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodRecord(record: MoodRecord)

    @Delete
    suspend fun deleteMoodRecord(record: MoodRecord)

    @Query("DELETE FROM mood_records")
    suspend fun clearAllMoodRecords()
}

@Dao
interface ReflectionDao {
    @Query("SELECT * FROM reflection_summaries ORDER BY timestamp DESC")
    fun getAllReflections(): Flow<List<ReflectionSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: ReflectionSummary)

    @Delete
    suspend fun deleteReflection(reflection: ReflectionSummary)

    @Query("DELETE FROM reflection_summaries")
    suspend fun clearAllReflections()
}

@Database(entities = [ChatMessage::class, MemoryItem::class, UserProfile::class, MoodRecord::class, ReflectionSummary::class], version = 6, exportSchema = false)
abstract class SaarthiDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao
    abstract fun profileDao(): ProfileDao
    abstract fun moodDao(): MoodDao
    abstract fun reflectionDao(): ReflectionDao
}

class SaarthiRepository(private val db: SaarthiDatabase) {
    val chatMessages: Flow<List<ChatMessage>> = db.chatDao().getAllMessages()
    val memoryItems: Flow<List<MemoryItem>> = db.memoryDao().getAllMemories()
    val userProfile: Flow<UserProfile?> = db.profileDao().getProfile()
    val moodRecords: Flow<List<MoodRecord>> = db.moodDao().getAllMoodRecords()
    val reflectionSummaries: Flow<List<ReflectionSummary>> = db.reflectionDao().getAllReflections()

    suspend fun insertMessage(message: ChatMessage) {
        db.chatDao().insertMessage(message)
    }

    suspend fun clearChatHistory() {
        db.chatDao().clearHistory()
    }

    suspend fun insertMemory(memory: MemoryItem) {
        db.memoryDao().insertMemory(memory)
    }

    suspend fun deleteMemory(memory: MemoryItem) {
        db.memoryDao().deleteMemory(memory)
    }

    suspend fun deleteMemoryById(id: Int) {
        db.memoryDao().deleteMemoryById(id)
    }

    suspend fun clearAllMemories() {
        db.memoryDao().clearAllMemories()
    }

    suspend fun saveProfile(profile: UserProfile) {
        db.profileDao().saveProfile(profile)
    }

    suspend fun insertMoodRecord(record: MoodRecord) {
        db.moodDao().insertMoodRecord(record)
    }

    suspend fun deleteMoodRecord(record: MoodRecord) {
        db.moodDao().deleteMoodRecord(record)
    }

    suspend fun clearAllMoodRecords() {
        db.moodDao().clearAllMoodRecords()
    }

    suspend fun insertReflection(reflection: ReflectionSummary) {
        db.reflectionDao().insertReflection(reflection)
    }

    suspend fun deleteReflection(reflection: ReflectionSummary) {
        db.reflectionDao().deleteReflection(reflection)
    }

    suspend fun clearAllReflections() {
        db.reflectionDao().clearAllReflections()
    }
}
