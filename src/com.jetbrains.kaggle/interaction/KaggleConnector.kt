package com.jetbrains.kaggle.interaction

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.notification.impl.NotificationFullContent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.event.HyperlinkEvent


private const val credentialsLink = "https://github.com/Kaggle/kaggle-api#api-credentials"

object KaggleConnector {
  private val configDir = System.getenv("KAGGLE_CONFIG_DIR") ?: "${System.getProperty("user.home")}${File.separator}.kaggle"

  private val service: KaggleService?
    get() {
      val dispatcher = Dispatcher()
      dispatcher.maxRequests = 10
      val credentialsFile = File("$configDir${File.separator}kaggle.json")
      if (!credentialsFile.exists()) {
        KaggleNotification("Failed to find credentials. Please, follow the instructions " +
            "<a href=\"$credentialsLink\">here</a>", NotificationType.WARNING, true).notify(null)
        return null
      }
      val mapper = ObjectMapper()
      val kaggleCredentials = mapper.readValue(credentialsFile, KaggleCredentials::class.java)

      val okHttpClient = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(BasicAuthInterceptor(kaggleCredentials.username, kaggleCredentials.key))
        .dispatcher(dispatcher)
        .build()
      val retrofit = Retrofit.Builder()
        .baseUrl("https://www.kaggle.com/api/v1/")
        .addConverterFactory(JacksonConverterFactory.create())
        .client(okHttpClient)
        .build()

      return retrofit.create(KaggleService::class.java)
    }

  fun datasets(): List<Dataset>? {
    val kaggleService = KaggleConnector.service ?: return null
    return kaggleService.datasets().execute().body()
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
    val datasetFile = File(project.basePath +"/datasets/$filename.zip")
    FileUtil.createIfDoesntExist(datasetFile)
    FileUtil.writeToFile(datasetFile, datasetBytes)
    Notifications.Bus.notify(KaggleNotification(
      "Dataset \"${dataset.title}\" saved to the ${datasetFile.path}",
      NotificationType.INFORMATION,
      false
    ))
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