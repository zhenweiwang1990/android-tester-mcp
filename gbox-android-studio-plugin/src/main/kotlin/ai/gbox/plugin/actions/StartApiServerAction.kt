package ai.gbox.plugin.actions

import ai.gbox.plugin.services.GboxApiService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages

class StartApiServerAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val apiService = service<GboxApiService>()
        
        val currentStatus = apiService.getServerStatus()
        
        if (currentStatus.success) {
            val choice = Messages.showYesNoDialog(
                project,
                "API Server is already running. Do you want to stop it?",
                "API Server Running",
                "Stop Server",
                "Cancel",
                Messages.getQuestionIcon()
            )
            
            if (choice == Messages.YES) {
                val stopResult = apiService.stopApiServer()
                Messages.showInfoMessage(
                    project,
                    stopResult.message,
                    "Stop API Server"
                )
            }
        } else {
            val startResult = apiService.startApiServer()
            
            if (startResult.success) {
                Messages.showInfoMessage(
                    project,
                    "${startResult.message}\n\nAPI Endpoints available at http://localhost:8765/api/\n\n" +
                            "Available endpoints:\n" +
                            "• POST /api/start - Start Android app\n" +
                            "• POST /api/stop - Stop Android app\n" +
                            "• POST /api/rerun - Rerun Android app\n" +
                            "• POST /api/debug - Debug Android app\n" +
                            "• GET /api/configurations - Get run configurations\n" +
                            "• POST /api/select-configuration - Select run configuration\n" +
                            "• GET /api/status - Get server status",
                    "API Server Started"
                )
            } else {
                Messages.showErrorDialog(
                    project,
                    startResult.message,
                    "Start API Server Failed"
                )
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val apiService = service<GboxApiService>()
        val status = apiService.getServerStatus()
        
        e.presentation.text = if (status.success) {
            "Stop API Server"
        } else {
            "Start API Server"
        }
        
        e.presentation.description = if (status.success) {
            "Stop the Run-API-Server (currently running)"
        } else {
            "Start the Run-API-Server for external control"
        }
    }
}