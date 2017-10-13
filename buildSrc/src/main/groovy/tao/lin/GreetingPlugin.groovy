package tao.lin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Upload
import tao.lin.test.Test

import java.beans.XMLDecoder

class GreetingPlugin implements Plugin<Project> {

  private Configuration compile
  private Configuration moduleCompile

  @Override
  void apply(Project project) {
    project.extensions.create("gTest", GreetingPluginExtension)

//    def version = javaVersion()
//    println "$version"
//    def compilationOptionString = project.properties['android.optional.compilation'] ?: ""
//    compilationOptionString.split(',').each {
//    }
//    project.task('showPlugin') {
//      doLast {
//
//      }
//    }
//    project.task("renameVersion", type: RenameVersionTask)
//    project.tasks.getByName('clean').dependsOn('renameVersion')
//
//    project.task("printVersion") {
//      doFirst {
//        println(project.getVersion())
//      }
//    }
//    println 'plugin lib init'
//    project.gradle.taskGraph.whenReady {
////      println("when taskGraph ready $it.allTasks")
//      println("is contains assembleRelease ? ${project.gradle.taskGraph.hasTask(":$project.name:assembleRelease")}")
//      if (project.gradle.taskGraph.hasTask(":$project.name:assembleRelease")) {
//        it.allTasks.eachWithIndex {Task task, int index  ->
//          println("task: $task.name")
//          if (task.name.startsWith('nuwa')) {
//            task.enabled = false
//          }
//        }
//      }
//    }

    project.afterEvaluate {
      project.android.applicationVariants.each { variant ->
        //      if (variant.name.contians('debug')) {
//        def javaCompileName = "transformClassesWithDexFor${variant.name.capitalize()}"
//        def javaPreCompileTask = project.tasks.findByName(javaCompileName)
//        println("has $javaCompileName ? $javaPreCompileTask")
//        if (javaPreCompileTask) {
//          def testPreBuild = "testPreBuild${variant.name}"
//          project.task(testPreBuild) {
//            doLast {
//              Set<File> inputFiles = javaPreCompileTask.inputs.files.files
//              println inputFiles
//              inputFiles.each {
//                def path = it.absolutePath
//                println path
//              }
//            }
//          }
//          def testPrebuildTask = project.tasks[testPreBuild]
//          testPrebuildTask.dependsOn javaPreCompileTask.taskDependencies.getDependencies(javaPreCompileTask)
//          javaPreCompileTask.dependsOn testPrebuildTask
//        }

        project.tasks.each { task ->
          task.doFirst {
            println("--------------------- $task.name inputs ---------------- ")
            Set<File> inputFiles = task.inputs.files.files
            inputFiles.each {
              def path = it.absolutePath
              println path
            }
          }
        }
        //      }
      }
    }

//    project.afterEvaluate {
//      println("after projectEvaluate")
//    }
//    if (project.hasProperty('android')) {
//      println('find android')
//    project.afterEvaluate {
//      project.tasks['preBuild'].doLast {
//        def ext = project.extensions.gTest
//        println "message $ext.message"
//      }
//      project.android.applicationVariants.each { variant ->
////        def proguardTask = project.tasks.findByName("proguard${variant.name.capitalize()}")
////        println "proguardTask find ? ${proguardTask!=null}"
//        println "--------- print ${variant.name.capitalize()} tasks ---------"
//        project.tasks.all {
//          println it.name
//        }
//        new Test().print()
//        println "--------- task ${variant.name.capitalize()} has shown ---------"
//      }
//    }
//    }
//    compile = project.configurations.compile
//    moduleCompile = project.configurations.create('testIII')
////    compile.extendsFrom moduleCompile
//    compile.dependencies.all {
//      println it.name
//    }
//    project.gradle.addListener(new DependencyResolutionListener() {
//      @Override
//      void beforeResolve(ResolvableDependencies resolvableDependencies) {
//        println 'beforeResolve'
//        moduleCompile.dependencies.collect {
//          def moduleName = it.name
//          println "module name is : $moduleName"
//        }
//        project.gradle.removeListener(this)
//      }
//
//      @Override
//      void afterResolve(ResolvableDependencies resolvableDependencies) {}
//    })
  }

//  def javaVersion =  {
//    String version = System.properties['java.version']
//    if (version == null) {
//      return version
//    }
//
//    def indexOfDecimal = version.indexOf('.')
//    indexOfDecimal = version.indexOf('.', indexOfDecimal + 1)
//    if (indexOfDecimal != -1) {
//      version = version.substring(0, indexOfDecimal)
//    }
//
//    try {
//      def numericVersion = Double.parseDouble(version)
//      numericVersion
//    } catch (NumberFormatException e) {
//      version
//    }
//  }
}

class GreetingPluginExtension {
  def String message = 'hello extension'
}