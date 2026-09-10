package com.vibecoding.ai

import android.app.Application
import com.vibecoding.ai.data.AppDatabase
import com.vibecoding.ai.data.ProjectRepository

class VibeCodingApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { ProjectRepository(database.projectDao(), filesDir) }
}
