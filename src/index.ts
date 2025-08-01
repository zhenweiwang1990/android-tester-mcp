import "dotenv/config";
import { FastMCP } from "fastmcp";
import { z } from "zod";

const server = new FastMCP({
  name: "gbox-run-tool",
  version: "1.0.0",
});

// Helper function to make HTTP requests to the Android Studio plugin API
async function makeApiRequest(endpoint: string, data?: any): Promise<any> {
  const url = `http://localhost:8765/api/${endpoint}`;

  try {
    const response = await fetch(url, {
      method: data ? "POST" : "GET",
      headers: {
        "Content-Type": "application/json",
      },
      body: data ? JSON.stringify(data) : undefined,
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const result = await response.json();
    return result;
  } catch (error: any) {
    const originalMsg = error?.message ?? String(error);
    let hint = "";
    // Most common reason is that the API server isn't running in Android Studio
    if (
      originalMsg.includes("ECONNREFUSED") ||
      originalMsg.includes("fetch failed")
    ) {
      hint =
        "\nHint: Ensure the Gbox API server is running in Android Studio (Tools → Gbox → Start API Server).";
    }
    throw new Error(`API request failed: ${originalMsg}${hint}`);
  }
}

// Android Start App Tool
server.addTool({
  name: "android_start_app",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Start Android App",
  },
  description: "Start the Android application in the current project",
  parameters: z.object({
    projectPath: z
      .string()
      .optional()
      .describe("Optional path to the Android project"),
  }),
  execute: async ({ projectPath }: { projectPath?: string }) => {
    try {
      const response = await makeApiRequest("start", { projectPath });

      if (!response.success) {
        throw new Error(`Start app failed: ${response.message}`);
      }

      return {
        content: [
          {
            type: "text",
            text: `✅ Android app started successfully: ${response.message}${
              response.data?.configurationName
                ? `\nConfiguration: ${response.data.configurationName}`
                : ""
            }`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `❌ Error starting Android app: ${(error as Error).message}`,
          },
        ],
      };
    }
  },
});

// Android Stop App Tool
server.addTool({
  name: "android_stop_app",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Stop Android App",
  },
  description: "Stop the currently running Android application",
  parameters: z.object({
    projectPath: z
      .string()
      .optional()
      .describe("Optional path to the Android project"),
  }),
  execute: async ({ projectPath }: { projectPath?: string }) => {
    try {
      const response = await makeApiRequest("stop", { projectPath });

      if (!response.success) {
        throw new Error(`Stop app failed: ${response.message}`);
      }

      return {
        content: [
          {
            type: "text",
            text: `✅ Android app stopped successfully: ${response.message}`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `❌ Error stopping Android app: ${(error as Error).message}`,
          },
        ],
      };
    }
  },
});

// Android Rerun App Tool
server.addTool({
  name: "android_rerun_app",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Rerun Android App",
  },
  description: "Rerun the Android application (stop and start)",
  parameters: z.object({
    projectPath: z
      .string()
      .optional()
      .describe("Optional path to the Android project"),
  }),
  execute: async ({ projectPath }: { projectPath?: string }) => {
    try {
      const response = await makeApiRequest("rerun", { projectPath });

      if (!response.success) {
        throw new Error(`Rerun failed: ${response.message}`);
      }

      return {
        content: [
          {
            type: "text",
            text: `✅ Android app rerun successful: ${response.message}${
              response.data?.configurationName
                ? `\nConfiguration: ${response.data.configurationName}`
                : ""
            }`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `❌ Error rerunning Android app: ${(error as Error).message}`,
          },
        ],
      };
    }
  },
});

// Android Debug App Tool
server.addTool({
  name: "android_debug_app",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Debug Android App",
  },
  description: "Start debugging the Android application",
  parameters: z.object({
    projectPath: z
      .string()
      .optional()
      .describe("Optional path to the Android project"),
  }),
  execute: async ({ projectPath }: { projectPath?: string }) => {
    try {
      const response = await makeApiRequest("debug", { projectPath });

      if (!response.success) {
        throw new Error(`Debug start failed: ${response.message}`);
      }

      return {
        content: [
          {
            type: "text",
            text: `🐛 Android app debug session started: ${response.message}${
              response.data?.configurationName
                ? `\nConfiguration: ${response.data.configurationName}`
                : ""
            }`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `❌ Error starting debug session: ${
              (error as Error).message
            }`,
          },
        ],
      };
    }
  },
});

// Android Get Configurations Tool
server.addTool({
  name: "android_get_configurations",
  annotations: {
    openWorldHint: true,
    readOnlyHint: true,
    title: "Get Android Configurations",
  },
  description: "Get list of available Android run configurations",
  parameters: z.object({
    projectPath: z
      .string()
      .optional()
      .describe("Optional path to the Android project"),
  }),
  execute: async ({ projectPath }: { projectPath?: string }) => {
    try {
      const response = await makeApiRequest("configurations", { projectPath });

      if (response.success && response.data) {
        const configurations = response.data;
        return {
          content: [
            {
              type: "text",
              text: `📱 Available Android configurations (${
                configurations.length
              }):\n${configurations
                .map((config: string) => `• ${config}`)
                .join("\n")}`,
            },
          ],
        };
      } else {
        return {
          content: [
            {
              type: "text",
              text: `❌ Failed to get configurations: ${
                response.message || "Unknown error"
              }`,
            },
          ],
        };
      }
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `❌ Error getting configurations: ${
              (error as Error).message
            }`,
          },
        ],
      };
    }
  },
});

// Android Select Configuration Tool
server.addTool({
  name: "android_select_configuration",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Select Android Configuration",
  },
  description: "Select a specific Android run configuration",
  parameters: z.object({
    configurationName: z
      .string()
      .describe("Name of the configuration to select"),
    projectPath: z
      .string()
      .optional()
      .describe("Optional path to the Android project"),
  }),
  execute: async ({
    configurationName,
    projectPath,
  }: {
    configurationName: string;
    projectPath?: string;
  }) => {
    try {
      const response = await makeApiRequest("select-configuration", {
        configurationName,
        projectPath,
      });

      return {
        content: [
          {
            type: "text",
            text: response.success
              ? `✅ Configuration selected: ${response.message}`
              : `❌ Failed to select configuration: ${response.message}`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `❌ Error selecting configuration: ${
              (error as Error).message
            }`,
          },
        ],
      };
    }
  },
});

// Android API Server Status Tool
server.addTool({
  name: "android_api_status",
  annotations: {
    openWorldHint: true,
    readOnlyHint: true,
    title: "Android API Server Status",
  },
  description: "Check the status of the Android Studio plugin API server",
  parameters: z.object({}),
  execute: async () => {
    try {
      const response = await makeApiRequest("status");

      return {
        content: [
          {
            type: "text",
            text: response.success
              ? `✅ Android API server is running: ${response.message}`
              : `❌ Android API server error: ${response.message}`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `❌ Android API server is not running or not accessible: ${
              (error as Error).message
            }`,
          },
        ],
      };
    }
  },
});

server.start({
  transportType: "stdio",
});
