package com.jetbrains.kaggle.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.kaggle.KaggleDatasetsCache
import com.jetbrains.kaggle.KaggleIcons
import com.jetbrains.kaggle.ui.KaggleDialog

class ImportDataset : DumbAwareAction("&Import Kaggle Dataset", "&Import Kaggle Dataset", KaggleIcons.KaggleLogo) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return

    if (KaggleDatasetsCache.INSTANCE.updateInProgress) {
      ProgressManager.getInstance().run(object : Task.Modal(null, "Loading datasets", true) {
        override fun run(indicator: ProgressIndicator) {
          indicator.isIndeterminate = true
          while (true) {
            indicator.checkCanceled()
            Thread.sleep(500)
            if (!KaggleDatasetsCache.INSTANCE.updateInProgress) {
              break
            }
          }
        }
      })
    }
    val datasets = KaggleDatasetsCache.INSTANCE.datasetsCache
    val kaggleDialog = KaggleDialog(datasets, project)
    kaggleDialog.show()
  }

  override fun update(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    e.presentation.isEnabledAndVisible = project != null
  }
}
