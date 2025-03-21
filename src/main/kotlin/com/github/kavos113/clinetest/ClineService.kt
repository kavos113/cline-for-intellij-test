package com.github.kavos113.clinetest

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClineService(project: Project) {
    private lateinit var cline: Cline
}