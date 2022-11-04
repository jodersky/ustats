import $file.jmh
import jmh.Jmh
import mill._, scalalib._, scalafmt._, publish._, scalanativelib._

val scala3 = "3.1.2"
val scalaNative = "0.4.5"

trait Publish extends PublishModule {
  def publishVersion = "0.5.0"
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

trait Utest extends ScalaModule with TestModule {
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.11")
  def testFramework = "utest.runner.Framework"
}

trait UstatsModule
    extends ScalaModule
    with ScalafmtModule
    with Publish {
  def scalaVersion = scala3
  def artifactName = "ustats"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::geny::1.0.0"
  )
}

object ustats extends Module {

  object jvm extends UstatsModule {
    override def millSourcePath = super.millSourcePath / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-jvm")))
    object test extends Tests with Utest
  }
  object native extends UstatsModule with ScalaNativeModule {
    def scalaNativeVersion = scalaNative
    override def millSourcePath = super.millSourcePath / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-native")))
    object test extends Tests with Utest
  }

  object server extends ScalaModule with Publish {
    def scalaVersion = scala3
    def moduleDeps = Seq(ustats.jvm)
    def ivyDeps = Agg(
      ivy"io.undertow:undertow-core:2.3.0.Final"
    )
    object test extends Tests with Utest
  }

}

object benchmark extends ScalaModule with Jmh {
  def scalaVersion = scala3
  def moduleDeps = Seq(ustats.jvm)
}

object examples extends Module {

  trait Example extends ScalaModule {
    def scalaVersion = scala3
    def moduleDeps: Seq[ScalaModule] = Seq(ustats.jvm)
  }

  object cask extends Example {
    def ivyDeps = Agg(
      ivy"com.lihaoyi::cask:0.8.1"
    )
  }
  object cask2 extends Example {
    def ivyDeps = cask.ivyDeps
  }
  object probe extends Example {
    def moduleDeps = Seq(ustats.jvm, ustats.server)
  }
}
