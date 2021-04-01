import $file.jmh
import jmh.Jmh
import mill._, scalalib._, scalafmt._, publish._

val scala213 = "2.13.5"
val scala3 = "3.0.0-RC2"
val dottyCustomVersion = Option(sys.props("dottyVersion"))

trait Publish extends PublishModule {
  def publishVersion = "0.4.3"
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
  def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.8")
  def testFrameworks = Seq("utest.runner.Framework")
}

class UstatsModule(val crossScalaVersion: String)
    extends CrossScalaModule
    with ScalafmtModule
    with Publish {
  def artifactName = "ustats"
  object test extends Tests with Utest
  // FIXME: scaladoc 3 is not supported by mill yet. Remove the override
  // once it is.
  override def docJar =
    if (crossScalaVersion.startsWith("2")) super.docJar
    else T {
      val outDir = T.ctx().dest
      val javadocDir = outDir / 'javadoc
      os.makeDir.all(javadocDir)
      mill.api.Result.Success(mill.modules.Jvm.createJar(Agg(javadocDir))(outDir))
    }
}

object ustats extends Cross[UstatsModule]((Seq(scala213, scala3) ++ dottyCustomVersion): _*) {

  class UstatsServerModule(val crossScalaVersion: String)
      extends CrossScalaModule
      with ScalafmtModule
      with Publish {
    def artifactName = "ustats-server"
    def moduleDeps = Seq(ustats(crossScalaVersion))
    def ivyDeps = Agg(
      ivy"io.undertow:undertow-core:2.2.3.Final"
    )
    object test extends Tests with Utest
    // FIXME: scaladoc 3 is not supported by mill yet. Remove the override
    // once it is.
    override def docJar =
      if (crossScalaVersion.startsWith("2")) super.docJar
      else T {
        val outDir = T.ctx().dest
        val javadocDir = outDir / 'javadoc
        os.makeDir.all(javadocDir)
        mill.api.Result.Success(mill.modules.Jvm.createJar(Agg(javadocDir))(outDir))
      }
  }
  object server extends Cross[UstatsServerModule]((Seq(scala213, scala3) ++ dottyCustomVersion): _*)

}

object benchmark extends ScalaModule with Jmh {
  def scalaVersion = scala213
  def moduleDeps = Seq(ustats(scala213))
}

object examples extends Module {

  trait Example extends ScalaModule {
    def scalaVersion = scala213
    def moduleDeps: Seq[ScalaModule] = Seq(ustats(scala213))
  }

  object cask extends Example {
    def ivyDeps = Agg(
      ivy"com.lihaoyi::cask:0.7.9"
    )
  }
  object cask2 extends Example {
    def ivyDeps = cask.ivyDeps
  }
  object probe extends Example {
    def moduleDeps = Seq(ustats(scala213), ustats.server(scala213))
  }
}
