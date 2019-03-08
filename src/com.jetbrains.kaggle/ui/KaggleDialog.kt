package com.jetbrains.kaggle.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBList.createDefaultListModel
import com.intellij.util.PathUtil
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.kaggle.KaggleDatasetsCache
import com.jetbrains.kaggle.interaction.Dataset
import com.jetbrains.kaggle.interaction.DatasetTopic
import com.jetbrains.kaggle.interaction.KaggleConnector
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextPane
import javax.swing.ListSelectionModel
import javax.swing.text.html.HTMLEditorKit

class KaggleDialog(datasets: List<Dataset>, private val project: Project) : DialogWrapper(false) {
  private val panel: BorderLayoutPanel = BorderLayoutPanel()
  private val jbList = JBList(datasets)

  private val descriptionArea = JTextPane()

  init {
    title = "Choose Dataset"
    val splitPane = JBSplitter()

    jbList.installCellRenderer<Dataset> { dataset -> JLabel(dataset.title) }
    jbList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    jbList.addListSelectionListener {
      descriptionArea.text = "<html>${jbList.selectedValue.subtitle}" +
          "<br/><br/>" +
          "<a href=\"${jbList.selectedValue.url}\">${jbList.selectedValue.title}</a></html>"
    }
    jbList.setEmptyText("No datasets available")
    ListSpeedSearch(jbList) { it.title }
    descriptionArea.isEditable = false
    descriptionArea.background = UIUtil.getPanelBackground()
    descriptionArea.contentType = HTMLEditorKit().contentType
    descriptionArea.editorKit = UIUtil.JBWordWrapHtmlEditorKit()
    descriptionArea.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
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
    if (datasets.isNotEmpty()) {
      jbList.selectedIndex = 0
    }

    ApplicationManager.getApplication().messageBus.connect().subscribe(KaggleConnector.datasetsTopic,
      object : DatasetTopic {
        override fun cacheUpdateStarted() {
          jbList.setPaintBusy(true)
        }

        override fun cacheUpdated() {
          jbList.setPaintBusy(false)
          jbList.model = createDefaultListModel(KaggleDatasetsCache.INSTANCE.datasets)
        }
      })
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return jbList
  }

  override fun createCenterPanel(): JComponent = panel

  override fun doOKAction() {
    super.doOKAction()
    val dataset = jbList.selectedValue ?: return

    val ref = dataset.ref
    val initialFileName = if (ref.startsWith(dataset.ownerRef)) ref.substring("${dataset.ownerRef}/".length) else ref

    val filePath = Messages.showInputDialog(
      "Enter path:", "Download Dataset", null,
      project.basePath + "/datasets/$initialFileName.zip", FilePathValidator
    ) ?: return

    ProgressManager.getInstance().run(object : Task.Backgroundable(
      project, "Loading Selected Dataset",
      false, PerformInBackgroundOption.DEAF
    ) {
      override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        KaggleConnector.downloadDataset(filePath, dataset)
      }
    })
  }
}

object FilePathValidator : InputValidator {
  override fun checkInput(inputString: String): Boolean {
    val fileNames = StringUtil.split(FileUtil.toSystemIndependentName(inputString), "/")
    if (fileNames.isEmpty()) {
      return false
    }
    for (fileName in fileNames) {
      if (!PathUtil.isValidFileName(fileName)) {
        return false
      }
    }
    return true
  }

  override fun canClose(inputString: String): Boolean {
    return checkInput(inputString)
  }
}