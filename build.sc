import $file.jmh
import jmh.Jmh
import mill._, scalalib._, scalafmt._

object ustats extends ScalaModule with ScalafmtModule {
  def scalaVersion = "2.13.1"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::sourcecode:0.2.1"
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
