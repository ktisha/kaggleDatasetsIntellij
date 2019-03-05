package com.jetbrains.kaggle

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object KaggleIcons {
  private fun load(path: String): Icon {
    return IconLoader.getIcon(path, KaggleIcons::class.java)
  }
  val KaggleLogo = load("/icons/kaggle.svg") // 16x16
}