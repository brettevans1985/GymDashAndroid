package com.gymdash.companion.ui.fooddiary

import androidx.lifecycle.ViewModel
import com.gymdash.companion.domain.repository.FoodDiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FoodDiaryViewModel @Inject constructor(
    val repository: FoodDiaryRepository
) : ViewModel()
