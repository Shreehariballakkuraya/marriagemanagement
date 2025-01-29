package com.hari.management.repository

import com.hari.management.data.GuestCategory
import com.hari.management.data.GuestDao
import com.hari.management.data.GuestEntity
import com.hari.management.data.InvitationStatus
import kotlinx.coroutines.flow.Flow

class GuestRepository(private val guestDao: GuestDao) {
    val allGuests: Flow<List<GuestEntity>> = guestDao.getAllGuests()

    fun getAllCategories(): Flow<List<GuestCategory>> = guestDao.getAllCategories()

    suspend fun insert(guest: GuestEntity) {
        guestDao.insertGuest(guest)
    }

    suspend fun insertCategory(category: GuestCategory) {
        guestDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: GuestCategory) {
        guestDao.deleteCategory(category)
    }

    fun searchGuests(query: String): Flow<List<GuestEntity>> {
        return guestDao.searchGuests(query)
    }

    suspend fun getGuestById(id: Int): GuestEntity? {
        return guestDao.getGuestById(id)
    }

    suspend fun updateGuest(guest: GuestEntity) {
        guestDao.updateGuest(guest)
    }

    suspend fun updateGuestVerification(guestId: Int, isVerified: Boolean) {
        guestDao.updateGuestVerification(guestId, isVerified)
    }

    suspend fun updateGuestReminder(guestId: Int, reminderDate: Long) {
        guestDao.updateGuestReminder(guestId, reminderDate)
    }

    suspend fun deleteGuest(guest: GuestEntity) {
        guestDao.deleteGuest(guest)
    }

    suspend fun deleteGuestById(guestId: Int) {
        guestDao.deleteGuestById(guestId)
    }

    suspend fun deleteAllGuests() {
        guestDao.deleteAllGuests()
    }

    suspend fun updateGuestStatus(guestId: Int, status: InvitationStatus) {
        guestDao.updateGuestStatus(guestId, status.name)
    }

    fun getFilteredGuests(
        query: String?,
        status: InvitationStatus?,
        categoryId: Int?
    ): Flow<List<GuestEntity>> {
        return guestDao.getFilteredGuests(query, status, categoryId)
    }

    suspend fun updateGuestInteraction(guestId: Int, hasInteracted: Boolean) {
        guestDao.updateGuestInteraction(guestId, hasInteracted)
    }

    fun getInteractedGuests(): Flow<List<GuestEntity>> {
        return guestDao.getInteractedGuests()
    }
} 