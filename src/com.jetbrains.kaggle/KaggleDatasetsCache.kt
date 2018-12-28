package com.jetbrains.kaggle

import com.intellij.openapi.components.*
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.kaggle.interaction.Dataset
import com.jetbrains.kaggle.interaction.KaggleConnector

@State(name = "KaggleDatasetsCache", storages = [Storage(value = "datasets.xml", roamingType = RoamingType.DISABLED)])
class KaggleDatasetsCache : PersistentStateComponent<KaggleDatasetsCache> {
  var datasets: List<Dataset> = ContainerUtil.createConcurrentList()

  var lastTimeChecked: Long = 0

  val datasetsCache: List<Dataset>
    get() {
      if (datasets.isEmpty()) {
        updateKaggleCache()
      }
      return datasets
    }

  fun updateKaggleCache() {
    val datasets = KaggleConnector.datasets()
    if (datasets != null) {
      KaggleDatasetsCache.instance.datasets = datasets
    }
  }

  override fun getState(): KaggleDatasetsCache? {
    return this
  }

  override fun loadState(state: KaggleDatasetsCache) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    val instance: KaggleDatasetsCache
      get() = ServiceManager.getService(KaggleDatasetsCache::class.java)
  }
}
