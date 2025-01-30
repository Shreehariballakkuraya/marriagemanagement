package com.hari.management.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.hari.management.data.GuestEntity
import com.hari.management.data.GuestCategory
import com.hari.management.data.GuestDatabase
import com.hari.management.repository.GuestRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.hari.management.data.InvitationStatus
import com.hari.management.util.ReminderWorker

@OptIn(ExperimentalCoroutinesApi::class)
class GuestViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GuestRepository
    private val _guests = MutableStateFlow<List<GuestEntity>>(emptyList())
    val guests = _guests.asStateFlow()
    val categories: Flow<List<GuestCategory>>
    
    // Change LiveData to StateFlow
    private val _interactedGuests = MutableStateFlow<List<GuestEntity>>(emptyList())
    val interactedGuests = _interactedGuests.asStateFlow()

    init {
        val guestDao = GuestDatabase.getDatabase(application).guestDao()
        repository = GuestRepository(guestDao)
        categories = repository.getAllCategories()
        
        // Update the collection of interacted guests
        viewModelScope.launch {
            repository.getInteractedGuests().collect {
                _interactedGuests.value = it
            }
        }
        
        viewModelScope.launch {
            repository.getAllGuests().collect {
                _guests.value = it
            }
        }
    }

    // UI State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _showDatePicker = MutableStateFlow<Int?>(null)
    val showDatePicker = _showDatePicker.asStateFlow()
    
    private val _selectedStatus = MutableStateFlow<InvitationStatus?>(null)
    val selectedStatus = _selectedStatus.asStateFlow()

    private val _selectedCategory = MutableStateFlow<GuestCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()
    
    // Guest list with search functionality
    val guestsFiltered = combine(
        _searchQuery,
        _selectedStatus,
        _selectedCategory
    ) { query, status, category ->
        repository.getFilteredGuests(
            query = if (query.isEmpty()) null else query,
            status = status,
            categoryId = category?.id
        )
    }.flatMapLatest { it }.asLiveData()
    
    // Create operations
    fun addGuest(name: String, phoneNumber: String, categoryId: Int? = null) = viewModelScope.launch {
        val guest = GuestEntity(
            name = name,
            phoneNumber = phoneNumber,
            categoryId = categoryId
        )
        repository.insert(guest)
    }
    
    // Read operations
    fun searchGuests(query: String) {
        _searchQuery.value = query
    }
    
    suspend fun getGuestById(id: Int): GuestEntity? {
        return repository.getGuestById(id)
    }
    
    // Update operations
    fun updateGuest(guest: GuestEntity) {
        viewModelScope.launch {
            repository.updateGuest(guest)
        }
    }
    
    fun updateGuestVerification(guestId: Int, isVerified: Boolean) {
        viewModelScope.launch {
            repository.updateGuestVerification(guestId, isVerified)
        }
    }
    
    fun updateGuestReminder(guestId: Int, reminderDate: Long?) {
        viewModelScope.launch {
            val guest = getGuestById(guestId) ?: return@launch
            repository.updateGuestReminder(guestId, reminderDate)
            
            // Handle notification scheduling
            if (reminderDate != null) {
                ReminderWorker.scheduleReminder(
                    getApplication(),
                    guestId,
                    guest.name,
                    reminderDate
                )
            } else {
                ReminderWorker.cancelReminder(getApplication(), guestId)
            }
        }
    }
    
    fun updateGuestStatus(guestId: Int, status: InvitationStatus) {
        viewModelScope.launch {
            repository.updateGuestStatus(guestId, status)
        }
    }
    
    fun updateGuestInteraction(guestId: Int, hasInteracted: Boolean) {
        viewModelScope.launch {
            repository.updateGuestInteraction(guestId, hasInteracted)
        }
    }
    
    // Delete operations
    fun deleteGuest(guest: GuestEntity) {
        viewModelScope.launch {
            repository.deleteGuest(guest)
        }
    }
    
    fun deleteGuestById(guestId: Int) {
        viewModelScope.launch {
            repository.deleteGuestById(guestId)
        }
    }
    
    fun deleteAllGuests() {
        viewModelScope.launch {
            repository.deleteAllGuests()
        }
    }
    
    // Date picker handling
    fun showDatePickerFor(guestId: Int) {
        _showDatePicker.value = guestId
    }
    
    fun hideDatePicker() {
        _showDatePicker.value = null
    }

    // Add these functions for category management
    fun addCategory(category: GuestCategory) = viewModelScope.launch {
        repository.insertCategory(category)
    }

    fun deleteCategory(category: GuestCategory) = viewModelScope.launch {
        repository.deleteCategory(category)
    }

    fun setStatusFilter(status: InvitationStatus?) {
        _selectedStatus.value = status
    }

    fun setCategoryFilter(category: GuestCategory?) {
        _selectedCategory.value = category
    }

    fun insertGuest(guest: GuestEntity) {
        viewModelScope.launch {
            repository.insertGuest(guest)
        }
    }
}

class GuestViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GuestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GuestViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 