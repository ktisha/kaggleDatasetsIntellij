package com.jetbrains.kaggle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.text.DateFormatUtil

class KaggleUpdater : StartupActivity {
  override fun runActivity(project: Project) {
    val application = ApplicationManager.getApplication()
    if (application.isUnitTestMode) {
      return
    }
    if (checkNeeded()) {
      KaggleDatasetsCache.INSTANCE.lastTimeChecked = System.currentTimeMillis()
      application.executeOnPooledThread {
        KaggleDatasetsCache.INSTANCE.updateKaggleCache()
      }
    }
  }

  private fun checkNeeded(): Boolean {
    val datasetsCache = KaggleDatasetsCache.INSTANCE
    val timeDelta = System.currentTimeMillis() - datasetsCache.lastTimeChecked
    return Math.abs(timeDelta) >= EXPIRATION_TIMEOUT
  }

  companion object {
    private const val EXPIRATION_TIMEOUT = DateFormatUtil.DAY
  }
}
