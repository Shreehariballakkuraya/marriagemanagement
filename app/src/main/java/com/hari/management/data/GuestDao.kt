package com.hari.management.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GuestDao {
    @Query("SELECT * FROM guests ORDER BY name ASC")
    fun getAllGuests(): Flow<List<GuestEntity>>
    
    @Query("SELECT * FROM guests WHERE id = :guestId")
    suspend fun getGuestById(guestId: Int): GuestEntity?
    
    @Query("SELECT * FROM guests WHERE name LIKE '%' || :searchQuery || '%'")
    fun searchGuests(searchQuery: String): Flow<List<GuestEntity>>
    
    @Insert
    suspend fun insertGuest(guest: GuestEntity)
    
    @Update
    suspend fun updateGuest(guest: GuestEntity)
    
    @Query("UPDATE guests SET isInvitationVerified = :isVerified WHERE id = :guestId")
    suspend fun updateGuestVerification(guestId: Int, isVerified: Boolean)
    
    @Query("UPDATE guests SET reminderDate = :reminderDate WHERE id = :guestId")
    suspend fun updateGuestReminder(guestId: Int, reminderDate: Long?)
    
    @Delete
    suspend fun deleteGuest(guest: GuestEntity)
    
    @Query("DELETE FROM guests WHERE id = :guestId")
    suspend fun deleteGuestById(guestId: Int)
    
    @Query("DELETE FROM guests")
    suspend fun deleteAllGuests()

    @Query("SELECT * FROM guest_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<GuestCategory>>

    @Insert
    suspend fun insertCategory(category: GuestCategory)

    @Delete
    suspend fun deleteCategory(category: GuestCategory)

    @Query("SELECT * FROM guests WHERE categoryId = :categoryId")
    fun getGuestsByCategory(categoryId: Int): Flow<List<GuestEntity>>

    data class GuestWithCategory(
        @Embedded val guest: GuestEntity,
        @Relation(
            parentColumn = "categoryId",
            entityColumn = "id"
        )
        val category: GuestCategory?
    )

    @Transaction
    @Query("SELECT * FROM guests")
    fun getGuestsWithCategories(): Flow<List<GuestWithCategory>>

    @Query("UPDATE guests SET invitationStatus = :status WHERE id = :guestId")
    suspend fun updateGuestStatus(guestId: Int, status: String)

    @Query("""
        SELECT * FROM guests 
        WHERE (:query IS NULL OR name LIKE '%' || :query || '%')
        AND (:status IS NULL OR invitationStatus = :status)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY name ASC
    """)
    fun getFilteredGuests(
        query: String?,
        status: InvitationStatus?,
        categoryId: Int?
    ): Flow<List<GuestEntity>>

    @Query("UPDATE guests SET hasInteracted = :hasInteracted WHERE id = :guestId")
    suspend fun updateGuestInteraction(guestId: Int, hasInteracted: Boolean)

    @Query("SELECT * FROM guests WHERE hasInteracted = 1")
    fun getInteractedGuests(): Flow<List<GuestEntity>>
} 