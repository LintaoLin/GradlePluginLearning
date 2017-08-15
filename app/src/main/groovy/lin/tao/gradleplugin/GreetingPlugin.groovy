package lin.tao.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
class GreetingPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.afterEvaluate {
      project.android.applicationVariants.all { variant ->
        def buildTypeName = variant.buildType.name
//
//        def task = project.tasks.create "jar${buildTypeName.capitalize()}", Jar
//        def packageTask = project.tasks.findByName("package${buildTypeName.capitalize()}")
//        task.archiveName = 'base.jar'
//        task.dependsOn packageTask
//        packageTask.finalizedBy task
//        task.outputs.upToDateWhen { false }
//        variant.apkLibraries.each {
//          logger.info('apkLibraries ===> ' + it.absolutePath)
//          task.from zipTree(it)
//        }
//        task.destinationDir = file(project.buildDir.absolutePath + "/outputs/jar")
//        artifacts.add('archives', task)
      }
    }
  }
}

