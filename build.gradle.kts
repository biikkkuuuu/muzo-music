// Top-level build file where you can add configuration options common to all sub-projects/modules.
System.setProperty("org.sqlite.tmpdir", "C:/Users/VIKASH~1/AppData/Local/Temp")
System.setProperty("java.io.tmpdir", "C:/Users/VIKASH~1/AppData/Local/Temp")

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}