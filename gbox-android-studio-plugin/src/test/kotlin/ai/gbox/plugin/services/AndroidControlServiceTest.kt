package ai.gbox.plugin.services

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AndroidControlServiceTest {

    @Test
    fun `test service instantiation`() {
        // This is a basic test to ensure the service can be instantiated
        // In a real test environment with IntelliJ test framework, 
        // you would mock the project and run manager dependencies
        assertDoesNotThrow {
            AndroidControlService()
        }
    }

    @Test
    fun `test execution result data class`() {
        val result = AndroidControlService.ExecutionResult(
            success = true,
            message = "Test message",
            configurationName = "test-config"
        )
        
        assertTrue(result.success)
        assertEquals("Test message", result.message)
        assertEquals("test-config", result.configurationName)
    }
}