package scalatex

import java.nio.file.Paths

import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.io.VirtualFile
import scala.tools.nsc.{ Global, Phase }
import scala.tools.nsc.plugins.{ Plugin, PluginComponent }

class ScalaTexPlugin(val global: Global) extends Plugin {
  import global._
  println(options)
  val name = "scalatex-plugin"
  val description = "Enforces coding standards"
  val components = List[PluginComponent](DemoComponent)
  private object DemoComponent extends PluginComponent {
    val global = ScalaTexPlugin.this.global
    import global._

    override val runsAfter = List("parser")
    override val runsBefore = List("namer")

    val phaseName = "Demo"
    val scalatexRoot = "book/src/main/scalatex"
    override def newPhase(prev: Phase) = new GlobalPhase(prev) {
      override def run() = {
        def recursiveListFiles(f: java.io.File): Array[java.io.File] = {
          val (dirs, files) = f.listFiles().partition(_.isDirectory)
          files ++ dirs.flatMap(recursiveListFiles)
        }
        for (file <- recursiveListFiles(new java.io.File(scalatexRoot))) {
          val txt = io.Source.fromFile(file).mkString
          val fakeJfile = new java.io.File(file.getName)
          val virtualFile = new VirtualFile(file.getName) {
            override def file = fakeJfile
          }
          val sourceFile = new BatchSourceFile(virtualFile, txt)
          val unit = new CompilationUnit(sourceFile)
          val name = file.getName
          val objectName = name.slice(name.lastIndexOf('/'), name.lastIndexOf('.'))
          val pkgName =
            Paths.get(scalatexRoot)
                .relativize(file.getParentFile.toPath)
                .toString
                .split("/")
                .map(s => s"package $s")
                .mkString("\n")

          val shim = s"""
            $pkgName
            import scalatags.Text.all._
            object $objectName{
              def apply() = scalatex.twf("${file.getPath}")
            }
          """
          unit.body = global.newUnitParser(shim).parse()
          global.currentRun.compileLate(unit)
        }
      }

      def name: String = phaseName

      def apply(unit: global.CompilationUnit): Unit = {}
    }
  }
}
