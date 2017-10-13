import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction

class PrintTaskInputsTask extends DefaultTask {
  Task nameTask

  @TaskAction
  def printInputs() {
    if (nameTask) {

    }
  }
}