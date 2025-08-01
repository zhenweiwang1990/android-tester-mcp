package ai.gbox.plugin.actions

import ai.gbox.plugin.services.AndroidControlService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StartAppAction : AnAction(), DumbAware {
    private val actionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val androidService = service<AndroidControlService>()
        
        actionScope.launch {
            try {
                val result = androidService.startApp(project?.basePath).get()
                
                if (result.success) {
                    Messages.showInfoMessage(
                        project,
                        result.message + (result.configurationName?.let { "\nConfiguration: $it" } ?: ""),
                        "Start Android App"
                    )
                } else {
                    Messages.showErrorDialog(
                        project,
                        result.message,
                        "Start Android App Failed"
                    )
                }
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    "Unexpected error: ${e.message}",
                    "Start Android App Error"
                )
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null && project.isOpen
    }
}