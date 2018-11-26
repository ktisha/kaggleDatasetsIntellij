package com.jetbrains.kaggle.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.kaggle.interaction.KaggleConnector
import com.jetbrains.kaggle.ui.KaggleDialog

class ImportDataset : DumbAwareAction("&Import Kaggle dataset") {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val datasets = KaggleConnector.datasets() ?: return
    // TODO: handle paginated response, cache the result

    val kaggleDialog = KaggleDialog(datasets, project)
    kaggleDialog.show()
  }

  override fun update(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    e.presentation.isEnabledAndVisible = project != null
  }
}
