import $file.jmh
import jmh.Jmh
import mill._, scalalib._, scalafmt._, publish._

object ustats extends ScalaModule with ScalafmtModule with PublishModule {
  def scalaVersion = "2.13.1"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::sourcecode:0.2.1"
  )
  def publishVersion = "0.0.1"
  def pomSettings = PomSettings(
    description = "Simple metrics collection",
    organization = "io.crashbox",
    url = "https://github.com/jodersky/ustats",
    licenses = Seq(License.`BSD-3-Clause`),
    versionControl = VersionControl.github("jodersky", "ustats"),
    developers = Seq(
      Developer("jodersky", "Jakob Odersky", "https://github.com/jodersky")
    )
  )
  object test extends Tests with ScalafmtModule {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.4")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}

object benchmark extends ScalaModule with Jmh {
  def scalaVersion = "2.13.1"
  def moduleDeps = Seq(ustats)
}

object examples extends Module {

  trait Example extends ScalaModule {
    def scalaVersion = ustats.scalaVersion
    def moduleDeps = Seq(ustats)
  }

  object cask extends Example {
    def ivyDeps = Agg(
      ivy"com.lihaoyi::cask:0.5.6"
    )
  }
  object cask2 extends Example {
    def ivyDeps = cask.ivyDeps
  }
}
