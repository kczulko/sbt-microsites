package microsites

import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.packager.NativePackagerKeys
import com.typesafe.sbt.packager.MappingsHelper._
import com.typesafe.sbt.packager.universal.UniversalPlugin
import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.site.jekyll.JekyllPlugin
import sbt.Keys._
import sbt._
import sbt.plugins.IvyPlugin
import tut.Plugin._
import microsites.FileHelper._
import microsites.layouts.{DocsLayout, HomeLayout}

object MicrositesPlugin extends AutoPlugin with NativePackagerKeys {

  object autoImport extends MicrositeKeys

  import MicrositesPlugin.autoImport._
  import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._

  override def requires = IvyPlugin && SbtNativePackager && JekyllPlugin && UniversalPlugin

  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = tutSettings ++
    micrositeSettings ++
    Seq(
    mappings in Universal ++= directory("src/main/resources/microsite"),
    microsite := createResources(
      config = MicrositeSettings(
        name = micrositeName.value,
        description = micrositeDescription.value,
        author = micrositeAuthor.value,
        homepage = micrositeHomepage.value,
        twitter = micrositeTwitter.value,
        highlightTheme = micrositeHighlightTheme.value,
        micrositeImgDirectory = micrositeImgDirectory.value,
        micrositeCssDirectory = micrositeCssDirectory.value,
        micrositeExtratMdFiles = micrositeExtratMdFiles.value,
        micrositeBaseUrl = micrositeBaseUrl.value,
        palette = micrositePalette.value,
        githubOwner = micrositeGithubOwner.value,
        githubRepo = micrositeGithubRepo.value
      ), resourceManagedDir = (resourceManaged in Compile).value),
    sourceDirectory in Jekyll := resourceManaged.value / "main" / "jekyll",
    tutSourceDirectory := sourceDirectory.value / "main" / "tut",
    tutTargetDirectory := resourceManaged.value / "main" / "jekyll"
  )

  lazy val micrositeSettings = Seq(
    micrositeName := name.value,
    micrositeDescription := description.value,
    micrositeAuthor := organizationName.value,
    micrositeHomepage := homepage.value.map(_.toString).getOrElse(""),
    micrositeBaseUrl := "",
    micrositeTwitter := "",
    micrositeHighlightTheme := "tomorrow",
    micrositeImgDirectory := (resourceDirectory in Compile).value / "microsite" / "img",
    micrositeCssDirectory := (resourceDirectory in Compile).value / "microsite" / "css",
    micrositeExtratMdFiles := Seq.empty,
    micrositePalette := Map(
      "brand-primary" -> "#E05236",
      "brand-secondary" -> "#3F3242",
      "brand-tertiary" -> "#2D232F",
      "gray-dark" -> "#453E46",
      "gray" -> "#837F84",
      "gray-light" -> "#E3E2E3",
      "gray-lighter" -> "#F4F3F4",
      "white-color" -> "#FFFFFF"),
    micrositeGithubOwner := "47deg",
    micrositeGithubRepo := "sbt-microsites")

  def createResources(config: MicrositeSettings, resourceManagedDir: File): Seq[File] = {

    val targetDir: String = getPathWithSlash(resourceManagedDir)
    val pluginURL: URL = getClass.getProtectionDomain.getCodeSource.getLocation

    copyPluginResources(pluginURL, s"${targetDir}jekyll/", "_sass")
    copyPluginResources(pluginURL, s"${targetDir}jekyll/", "css")
    copyPluginResources(pluginURL, s"${targetDir}jekyll/", "js")

    copyFilesRecursively(config.micrositeImgDirectory.getAbsolutePath, s"${targetDir}jekyll/img/")
    copyFilesRecursively(config.micrositeCssDirectory.getAbsolutePath, s"${targetDir}jekyll/css/")

    config.micrositeExtratMdFiles foreach { f =>
      copyFilesRecursively(f.getAbsolutePath, s"${targetDir}jekyll/${f.getName.toLowerCase}")
    }

    Seq(createConfigYML(config, targetDir), createPalette(config, targetDir)) ++ createLayouts(config, targetDir)
  }

  def createConfigYML(config: MicrositeSettings, targetDir: String): File = {
    val targetFile = createFilePathIfNotExists(s"${targetDir}jekyll/_config.yml")

    val baseUrl = if(!config.micrositeBaseUrl.isEmpty && !config.micrositeBaseUrl.startsWith("/"))
      s"/${config.micrositeBaseUrl}"
    else config.micrositeBaseUrl

    IO.write(targetFile, s"""name: ${config.name}
                             |description: "${config.description}"
                             |baseurl: $baseUrl
                             |docs: true
                             |
                             |markdown: kramdown
                             |collections:
                             |  tut:
                             |    output: true
                             |""".stripMargin)

    targetFile
  }

  def createPalette(config: MicrositeSettings, targetDir: String): File = {
    val targetFile = createFilePathIfNotExists(s"${targetDir}jekyll/_sass/_variables_palette.scss")
    val content = config.palette.map{case (key, value) => s"""$$$key: $value;"""}.mkString("\n")
    IO.write(targetFile, content)
    targetFile
  }

  def createLayouts(config: MicrositeSettings, targetDir: String): Seq[File] =
    List("home" -> HomeLayout, "docs" -> DocsLayout) map { case (layoutName, layout) =>
      val targetFile = createFilePathIfNotExists(s"${targetDir}jekyll/_layouts/$layoutName.html")
      IO.write(targetFile, layout.render(config).toString())
      targetFile
    }

}