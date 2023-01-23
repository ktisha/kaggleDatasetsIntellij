package com.jetbrains.kaggle.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.kaggle.KaggleDatasetsCache
import com.jetbrains.kaggle.KaggleIcons
import com.jetbrains.kaggle.interaction.CREDENTIALS_MESSAGE
import com.jetbrains.kaggle.interaction.KaggleConnector
import com.jetbrains.kaggle.interaction.KaggleNotification
import com.jetbrains.kaggle.ui.KaggleDialog
import java.io.File

class ImportDataset : DumbAwareAction("&Import Kaggle Dataset", "Import kaggle dataset", KaggleIcons.KaggleLogo) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return

    val credentialsFile = File("${KaggleConnector.configDir}${File.separator}kaggle.json")
    if (!credentialsFile.exists()) {
      KaggleNotification(
        CREDENTIALS_MESSAGE, NotificationType.WARNING, true
      ).notify(null)
      return
    }

    ProgressManager.getInstance().run(object : Task.Backgroundable(
      null, "Import dataset",
      true, PerformInBackgroundOption.DEAF
    ) {
      override fun run(indicator: ProgressIndicator) {
        val datasets = KaggleDatasetsCache.INSTANCE.datasetsCache
        indicator.isIndeterminate = false
        indicator.text = "Loading datasets"
        indicator.text2 = "It may take some time to load datasets for the first time"
        while (true) {
          indicator.checkCanceled()
          Thread.sleep(500)
          if (!KaggleDatasetsCache.INSTANCE.updateInProgress) {
            break
          }
        }
        runInEdt {
          val kaggleDialog = KaggleDialog(datasets, project)
          kaggleDialog.show()
        }
      }
    })
  }

  override fun update(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    e.presentation.isEnabledAndVisible = project != null
  }
}
