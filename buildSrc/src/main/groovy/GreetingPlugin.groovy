import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies

class GreetingPlugin implements Plugin<Project> {

  private Configuration compile
  private Configuration moduleCompile

  @Override
  void apply(Project project) {
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
    println 'plugin lib init'
    compile = project.configurations.compile
    moduleCompile = project.configurations.create('testIII')
//    compile.extendsFrom moduleCompile
    compile.dependencies.all {
      println it.name
    }
    project.gradle.addListener(new DependencyResolutionListener() {
      @Override
      void beforeResolve(ResolvableDependencies resolvableDependencies) {
        println 'beforeResolve'
        moduleCompile.dependencies.collect {
          def moduleName = it.name
          println "module name is : $moduleName"
        }
        project.gradle.removeListener(this)
      }

      @Override
      void afterResolve(ResolvableDependencies resolvableDependencies) {}
    })
  }

  def javaVersion =  {
    String version = System.properties['java.version']
    if (version == null) {
      return version
    }

    def indexOfDecimal = version.indexOf('.')
    indexOfDecimal = version.indexOf('.', indexOfDecimal + 1)
    if (indexOfDecimal != -1) {
      version = version.substring(0, indexOfDecimal)
    }

    try {
      def numericVersion = Double.parseDouble(version)
      numericVersion
    } catch (NumberFormatException e) {
      version
    }
  }
}

class GreetingPluginExtension {
  def String message = 'hello extension'
}