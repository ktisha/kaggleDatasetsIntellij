package com.jetbrains.kaggle.ui

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.kaggle.interaction.Dataset
import com.jetbrains.kaggle.interaction.KaggleConnector
import javax.swing.JComponent
import javax.swing.JLabel

class KaggleDialog(datasets: List<Dataset>, private val project: Project) : DialogWrapper(false) {
  private val panel: BorderLayoutPanel = BorderLayoutPanel()
  private val jbList = JBList(datasets)

  init {
    title = "Choose dataset"
    jbList.installCellRenderer<Dataset> { dataset -> JLabel(dataset.title) }

    panel.add(JBScrollPane(jbList))
    init()
  }

  override fun createCenterPanel(): JComponent = panel

  override fun doOKAction() {
    super.doOKAction()
    val dataset = jbList.selectedValue
    ProgressManager.getInstance().run(object : Task.Modal(project, "Loading Selected Dataset", false) {
      override fun run(indicator: ProgressIndicator) {
        KaggleConnector.downloadDataset(dataset, project)
      }
    })
  }
}