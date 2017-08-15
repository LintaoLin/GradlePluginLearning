import org.gradle.api.Plugin
import org.gradle.api.Project

class GreetingPlugin implements Plugin<Project> {


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
    project.task("renameVersion", type: RenameVersionTask)
    project.tasks.getByName('clean').dependsOn('renameVersion')
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