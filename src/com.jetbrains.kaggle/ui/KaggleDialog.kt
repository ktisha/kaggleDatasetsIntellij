package com.jetbrains.kaggle.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.AnActionButton
import com.intellij.ui.JBSplitter
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBList.createDefaultListModel
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.kaggle.KaggleDatasetsCache
import com.jetbrains.kaggle.interaction.Dataset
import com.jetbrains.kaggle.interaction.DatasetTopic
import com.jetbrains.kaggle.interaction.KaggleConnector
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.ListSelectionModel

class KaggleDialog(datasets: List<Dataset>, private val project: Project) : DialogWrapper(false) {
  private val panel: BorderLayoutPanel = BorderLayoutPanel()
  private val jbList = JBList(datasets)

  private val descriptionArea = JTextArea()

  init {
    title = "Choose dataset"
    val splitPane = JBSplitter()

    jbList.installCellRenderer<Dataset> { dataset -> JLabel(dataset.title) }
    jbList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    jbList.addListSelectionListener {
      descriptionArea.text = jbList.selectedValue.subtitle
    }
    jbList.setEmptyText("No datasets available")
    if (datasets.isNotEmpty()) {
      jbList.selectedIndex = 0
    }
    ListSpeedSearch(jbList) { it.title }
    descriptionArea.lineWrap = true
    descriptionArea.isEditable = false
    descriptionArea.background = UIUtil.getPanelBackground()
    splitPane.firstComponent =
      ToolbarDecorator.createDecorator(jbList).disableAddAction().disableRemoveAction().disableUpDownActions()
        .addExtraAction(object : AnActionButton("Refresh datasets list", AllIcons.Actions.Refresh) {
          override fun actionPerformed(e: AnActionEvent) {
            KaggleDatasetsCache.INSTANCE.updateKaggleCache()
          }
        }).createPanel()
    splitPane.secondComponent = descriptionArea
    panel.add(splitPane)
    panel.preferredSize = Dimension(800, 600)
    init()

    ApplicationManager.getApplication().messageBus.connect().subscribe(KaggleConnector.datasetsTopic,
      object : DatasetTopic {
        override fun datasetCacheUpdateStarted() {
          jbList.setPaintBusy(true)
        }

        override fun datasetCacheUpdated() {
          jbList.setPaintBusy(false)
          jbList.model = createDefaultListModel(KaggleDatasetsCache.INSTANCE.datasetsCache)
        }
      })
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return jbList
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