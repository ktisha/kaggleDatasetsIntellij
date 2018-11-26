package com.jetbrains.kaggle.ui

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
    KaggleConnector.downloadDataset(dataset, project)
    // TODO: download in background to the dataset directory and show notification
  }
}