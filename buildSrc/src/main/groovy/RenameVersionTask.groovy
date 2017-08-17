import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class RenameVersionTask extends DefaultTask {

  @TaskAction
  def rename() {
    println('into method')
    if (project.hasProperty('android')) {
      println('find android')
      project.android.applicationVariants.all { variant ->
        if (variant.buildType.name == project.android.buildTypes.debug.name) {
          def customVersionName = variant.mergedFlavor.versionName
          variant.mergedFlavor.versionName = customVersionName + ' custom'
          println 'version name change'
          println getBranchName()
          println versionName()
        }
      }
    }
  }

  def getBranchName() {
    def currentBranchName = 'git rev-parse --abbrev-ref HEAD'.execute().text.trim()
    currentBranchName
  }

  def versionName() {
    project.version
  }

  RenameVersionTask() {
    group = 'greeting'
    description = 'Renames versionName of the app'
  }
}