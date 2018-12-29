package com.jetbrains.kaggle.interaction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@Suppress("unused")
interface KaggleService {

  @GET("datasets/list")
  fun datasets(@Query("page") page: Int): Call<List<Dataset>>

  @GET("datasets/list/{ref}")
  fun dataset(@Path("ref") ref: String): Call<DatasetFilesContainer>

  @Streaming
  @GET("datasets/download/{ref}")
  fun downloadDataset(@Path("ref") ref: String): Call<ResponseBody>
}

@Suppress("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
class Dataset {
  var id: Int = 0
  lateinit var ref: String
  lateinit var ownerRef: String
  var currentVersionNumber: Int = 0
  lateinit var title: String
  lateinit var subtitle: String
  lateinit var url: String
  var isPrivate: Boolean = false
}

@Suppress("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
class DatasetFilesContainer {
  lateinit var datasetFiles: List<DatasetFiles>
}

@Suppress("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
class DatasetFiles {
  lateinit var ref: String
}

@Suppress("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
class KaggleCredentials {
  lateinit var username: String
  lateinit var key: String
}
