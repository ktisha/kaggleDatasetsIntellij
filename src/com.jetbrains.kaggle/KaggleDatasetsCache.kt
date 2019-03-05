package com.jetbrains.kaggle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.kaggle.interaction.Dataset
import com.jetbrains.kaggle.interaction.DatasetTopic
import com.jetbrains.kaggle.interaction.KaggleConnector

@State(name = "KaggleDatasetsCache", storages = [Storage(value = "datasets.xml", roamingType = RoamingType.DISABLED)])
class KaggleDatasetsCache : PersistentStateComponent<KaggleDatasetsCache> {
  @Volatile var datasets: MutableList<Dataset> = mutableListOf()
  @Volatile var lastTimeChecked: Long = 0

  @get:Transient
  @set:Transient
  @Volatile var updateInProgress: Boolean = false

  val datasetsCache: List<Dataset>
    get() {
      if (datasets.isEmpty()) {
        updateKaggleCache()
      }
      return datasets
    }

  fun updateKaggleCache() {
    ApplicationManager.getApplication().messageBus.syncPublisher<DatasetTopic>(KaggleConnector.datasetsTopic).datasetCacheUpdateStarted()
    if (updateInProgress) return
    updateInProgress = true
    KaggleConnector.fillDatasets()
  }

  override fun getState(): KaggleDatasetsCache? {
    return this
  }

  override fun loadState(state: KaggleDatasetsCache) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    val INSTANCE: KaggleDatasetsCache
      get() = ServiceManager.getService(KaggleDatasetsCache::class.java)
  }
}
