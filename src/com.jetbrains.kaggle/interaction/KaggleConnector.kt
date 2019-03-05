package com.jetbrains.kaggle.interaction

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.notification.impl.NotificationFullContent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.messages.Topic
import com.jetbrains.kaggle.KaggleDatasetsCache
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.event.HyperlinkEvent


const val credentialsLink = "https://github.com/Kaggle/kaggle-api#api-credentials"

object KaggleConnector {
  private val LOG = Logger.getInstance(KaggleConnector::class.java.name)
  val datasetsTopic = Topic.create<DatasetTopic>("Kaggle.datasets", DatasetTopic::class.java)
  private const val PAGES_TO_LOAD = 300
  val configDir =
    System.getenv("KAGGLE_CONFIG_DIR") ?: "${System.getProperty("user.home")}${File.separator}.kaggle"

  private val service: KaggleService?
    get() {
      val dispatcher = Dispatcher()
      dispatcher.maxRequests = 10
      val credentialsFile = File("$configDir${File.separator}kaggle.json")
      if (!credentialsFile.exists()) {
        KaggleNotification(
          "Failed to find credentials. Please, follow the instructions " +
              "<a href=\"$credentialsLink\">here</a>", NotificationType.WARNING, true
        ).notify(null)
        return null
      }
      val mapper = ObjectMapper()
      val kaggleCredentials = mapper.readValue(credentialsFile, KaggleCredentials::class.java)

      val logger = HttpLoggingInterceptor { LOG.info(it) }
      logger.level =
        if (ApplicationManager.getApplication().isInternal) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC

      val okHttpClient = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(BasicAuthInterceptor(kaggleCredentials.username, kaggleCredentials.key))
        .addInterceptor(logger)
        .dispatcher(dispatcher)
        .build()
      val retrofit = Retrofit.Builder()
        .baseUrl("https://www.kaggle.com/api/v1/")
        .addConverterFactory(JacksonConverterFactory.create())
        .client(okHttpClient)
        .build()

      return retrofit.create(KaggleService::class.java)
    }

  fun fillDatasets() {
    val kaggleService = KaggleConnector.service ?: return
    var pagesLoaded = 0
    for (currentPage in 1..PAGES_TO_LOAD) {
      kaggleService.datasets(currentPage).enqueue(object : Callback<List<Dataset>> {
        override fun onFailure(call: Call<List<Dataset>>, t: Throwable) {
          pageLoaded()
          LOG.warn("Failed to get $currentPage page")
        }

        override fun onResponse(call: Call<List<Dataset>>, response: retrofit2.Response<List<Dataset>>) {
          pageLoaded()
          val datasets = response.body() ?: return
          if (datasets.isEmpty()) return
          KaggleDatasetsCache.INSTANCE.datasets.addAll(datasets)
        }

        private fun pageLoaded() {
          pagesLoaded += 1
          if (pagesLoaded == PAGES_TO_LOAD - 1) {
            KaggleDatasetsCache.INSTANCE.updateInProgress = false
            ApplicationManager.getApplication().messageBus.syncPublisher<DatasetTopic>(datasetsTopic)
              .datasetCacheUpdated()
          }
        }
      })
    }
  }

  fun downloadDataset(dataset: Dataset, project: Project) {
    val kaggleService = KaggleConnector.service ?: return
    val responseBody = kaggleService.downloadDataset(dataset.ref).execute().body()
    val datasetBytes = responseBody?.bytes() ?: return
    val filename = if (dataset.ref.startsWith(dataset.ownerRef))
      dataset.ref.substring("${dataset.ownerRef}/".length)
    else
      dataset.ref

    // TODO: use correct path, check if file is unique
    val datasetFile = File(project.basePath + "/datasets/$filename.zip")
    FileUtil.createIfDoesntExist(datasetFile)
    FileUtil.writeToFile(datasetFile, datasetBytes)
    Notifications.Bus.notify(
      KaggleNotification(
        "Dataset \"${dataset.title}\" saved to the ${datasetFile.path}",
        NotificationType.INFORMATION,
        false
      )
    )
  }
}

class KaggleNotification(content: String, type: NotificationType, hyperlink: Boolean) :
  Notification("kaggle.downloader",
    "", content, type,
    object : NotificationListener.Adapter() {
      override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        if (hyperlink) {
          BrowserUtil.browse(credentialsLink)
        }
        notification.expire()
      }
    }), NotificationFullContent

class BasicAuthInterceptor(user: String, token: String) : Interceptor {

  private val credentials: String = Credentials.basic(user, token)

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val authenticatedRequest = request.newBuilder()
      .header("Authorization", credentials).build()
    return chain.proceed(authenticatedRequest)
  }
}

interface DatasetTopic {
  fun datasetCacheUpdateStarted()
  fun datasetCacheUpdated()
}