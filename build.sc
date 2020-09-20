import $file.jmh
import jmh.Jmh
import mill._, scalalib._, scalafmt._, publish._

trait Publish extends PublishModule {
  def publishVersion = "0.1.0"
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
}

object ustats extends ScalaModule with ScalafmtModule with Publish {
  def scalaVersion = "2.13.2"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::sourcecode:0.2.1"
  )

  object test extends Tests with ScalafmtModule {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.4")
    def testFrameworks = Seq("utest.runner.Framework")
  }

  object server extends ScalaModule with ScalafmtModule with Publish {
    def moduleDeps = Seq(ustats)
    def scalaVersion = ustats.scalaVersion()
    def ivyDeps = Agg(
      ivy"io.undertow:undertow-core:2.1.0.Final"
    )
    object test extends Tests with ScalafmtModule {
      def ivyDeps = Agg(
        ivy"com.lihaoyi::requests:0.6.5",
        ivy"com.lihaoyi::utest:0.7.4"
      )

      def testFrameworks = Seq("utest.runner.Framework")
    }
  }

}

object benchmark extends ScalaModule with Jmh {
  def scalaVersion = ustats.scalaVersion
  def moduleDeps = Seq(ustats)
}

object examples extends Module {

  trait Example extends ScalaModule {
    def scalaVersion = ustats.scalaVersion
    def moduleDeps = Seq(ustats)
  }

  object cask extends Example {
    def ivyDeps = Agg(
      ivy"com.lihaoyi::cask:0.6.5"
    )
  }
  object cask2 extends Example {
    def ivyDeps = cask.ivyDeps
  }
}
