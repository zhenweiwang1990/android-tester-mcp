import "dotenv/config";
import GboxSDK, {
  AndroidBoxOperator,
  ActionOperator,
  CreateAndroid,
} from "gbox-sdk";

import { FastMCP } from "fastmcp";
import { z } from "zod";
import { exec } from "child_process";

// Ensure API KEY is available
const apiKey = process.env.GBOX_API_KEY;
if (!apiKey) {
  throw new Error("请在环境变量或 .env 文件中设置 GBOX_API_KEY");
}

// Initialise Gbox SDK
const gboxSDK = new GboxSDK({ apiKey });

interface AIActionResult {
  screenshot: {
    after: { uri: string };
    before: { uri: string };
    trace: { uri: string };
  };
  // Other fields may be present, but we only use screenshot data for now.
}

type ActionOperatorWithAI = ActionOperator & {
  ai: (instruction: string) => Promise<AIActionResult>;
};

type AndroidBoxWithAI = AndroidBoxOperator & {
  action: ActionOperatorWithAI;
};

async function attachBox(boxId: string): Promise<AndroidBoxWithAI> {
  try {
    const box = (await gboxSDK.get(boxId)) as AndroidBoxWithAI;
    return box;
  } catch (err) {
    throw new Error(
      `Failed to attach to box ${boxId}: ${(err as Error).message}`
    );
  }
}

const createAndroidBoxParamsSchema = z
  .object({
    config: z
      .object({
        deviceType: z
          .enum(["virtual", "physical"])
          .optional()
          .describe("Device type - virtual or physical Android device"),
        envs: z
          .record(z.string())
          .optional()
          .describe("Environment variables for the box."),
        expiresIn: z
          .string()
          .regex(/^\d+(ms|s|m|h)$/)
          .optional()
          .describe(
            'The box will be alive for the given duration (e.g., "30s", "5m", "1h"). Default: 60m'
          ),
        labels: z
          .record(z.string())
          .optional()
          .describe("Key-value pairs of labels for the box."),
      })
      .optional(),
    wait: z
      .boolean()
      .optional()
      .describe("Wait for the box operation to be completed, default is true"),
  })
  .partial()
  .describe("Parameters for creating a new Android box.");

const server = new FastMCP({
  name: "gbox-android",
  version: "1.0.0",
});

// Zod schema derived from BoxListParams in gbox-sdk
const listBoxesParamsSchema = z
  .object({
    deviceType: z
      .string()
      .optional()
      .describe("Filter boxes by their device type (virtual, physical)"),
    labels: z
      .any()
      .optional()
      .describe(
        "Filter boxes by their labels. Labels are key-value pairs that help identify and categorize boxes."
      ),
    page: z.number().int().optional().describe("Page number"),
    pageSize: z.number().int().optional().describe("Page size"),
    status: z
      .array(z.enum(["all", "pending", "running", "error", "terminated"]))
      .optional()
      .describe(
        "Filter boxes by their current status (pending, running, stopped, error, terminated, all)."
      ),
    type: z
      .array(z.enum(["all", "linux", "android"]))
      .optional()
      .describe(
        "Filter boxes by their type (linux, android, all). Must be an array of types."
      ),
  })
  .partial();

server.addTool({
  name: "create_android_box",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Create Gbox Android",
  },
  description: "Create a fresh Android box and return its metadata.",
  parameters: createAndroidBoxParamsSchema,
  execute: async (params: z.infer<typeof createAndroidBoxParamsSchema>) => {
    const created = await gboxSDK.create({
      type: "android",
      ...params,
    } as CreateAndroid);
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(created.data, null, 2),
        },
      ],
    };
  },
});

server.addTool({
  name: "list_boxes",
  annotations: {
    openWorldHint: true,
    readOnlyHint: true,
    title: "List Gbox Android Boxes",
  },
  description: "List all current boxes belonging to this API Key.",
  parameters: listBoxesParamsSchema,
  execute: async (query: z.infer<typeof listBoxesParamsSchema>) => {
    const boxes = await gboxSDK.listInfo(query);
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(boxes, null, 2),
        },
      ],
    };
  },
});

server.addTool({
  name: "get_box",
  annotations: {
    openWorldHint: true,
    readOnlyHint: true,
    title: "Get Gbox Android Box",
  },
  description: "Get box information by ID.",
  parameters: z.object({
    boxId: z.string().describe("ID of the box"),
  }),
  execute: async ({ boxId }: { boxId: string }) => {
    const info = await gboxSDK.getInfo(boxId);
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(info, null, 2),
        },
      ],
    };
  },
});

server.addTool({
  name: "get_screenshot",
  annotations: {
    openWorldHint: true,
    readOnlyHint: true,
    title: "Get Gbox Android Screenshot",
  },
  description: "Take a screenshot of the current display for a given box.",
  parameters: z.object({
    boxId: z.string().describe("ID of the box"),
    outputFormat: z
      .enum(["base64", "storageKey"])
      .optional()
      .default("base64")
      .describe("The output format for the screenshot."),
  }),
  execute: async ({
    boxId,
    outputFormat,
  }: {
    boxId: string;
    outputFormat?: "base64" | "storageKey";
  }) => {
    const box = await attachBox(boxId);
    // Ensure we request base64 output for easy embedding
    const result = await box.action.screenshot({ outputFormat });

    // The SDK returns a `uri` string. It may be a bare base64 string or a data URI.
    let mimeType = "image/png";
    let base64Data = result.uri;

    if (result.uri.startsWith("data:")) {
      const match = result.uri.match(/^data:(.+);base64,(.*)$/);
      if (match) {
        mimeType = match[1];
        base64Data = match[2];
      }
    }

    return {
      content: [
        {
          type: "image",
          data: base64Data,
          mimeType,
        },
      ],
    };
  },
});

server.addTool({
  name: "ai_action",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "AI Action",
  },
  description:
    "Perform an action on the UI of the android box (natural language instruction).",
  parameters: z.object({
    boxId: z.string().describe("ID of the box"),
    instruction: z
      .string()
      .describe(
        "Direct instruction of the UI action to perform, e.g. 'click the login button'"
      ),
    background: z
      .string()
      .optional()
      .describe(
        "Contextual background for the action, to help the AI understand previous steps"
      ),
    includeScreenshot: z
      .boolean()
      .optional()
      .describe(
        "Whether to include screenshots in the action response (default false)"
      ),
    outputFormat: z
      .enum(["base64", "storageKey"])
      .optional()
      .describe("Output format for screenshot URIs (default 'base64')"),
    screenshotDelay: z
      .string()
      .regex(/^[0-9]+(ms|s|m|h)$/)
      .optional()
      .describe(
        "Delay after performing the action before the final screenshot, e.g. '500ms'"
      ),
  }),
  execute: async ({
    boxId,
    ...actionParams
  }: {
    boxId: string;
    instruction: string;
    background?: string;
    includeScreenshot?: boolean;
    outputFormat?: "base64" | "storageKey";
    screenshotDelay?: string;
  }) => {
    const box = await attachBox(boxId);

    // Ensure screenshots are included and returned as base64 for easy embedding
    const {
      includeScreenshot = true,
      outputFormat = "base64",
      ...restParams
    } = actionParams as any;

    const result = (await box.action.ai({
      includeScreenshot,
      outputFormat,
      ...restParams,
    } as any)) as AIActionResult;

    // Prepare image contents for before and after screenshots
    const images: Array<{ type: "image"; data: string; mimeType: string }> = [];

    const parseUri = (uri: string) => {
      let mimeType = "image/png";
      let base64Data = uri;

      if (uri.startsWith("data:")) {
        const match = uri.match(/^data:(.+);base64,(.*)$/);
        if (match) {
          mimeType = match[1];
          base64Data = match[2];
        }
      }

      return { mimeType, base64Data };
    };

    if (result?.screenshot?.before?.uri) {
      const { mimeType, base64Data } = parseUri(result.screenshot.before.uri);
      images.push({ type: "image", data: base64Data, mimeType });
    }

    if (result?.screenshot?.after?.uri) {
      const { mimeType, base64Data } = parseUri(result.screenshot.after.uri);
      images.push({ type: "image", data: base64Data, mimeType });
    }

    // Fallback to text if no images were produced
    if (images.length === 0) {
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(result, null, 2),
          },
        ],
      };
    }

    return { content: images };
  },
});

server.addTool({
  name: "install_apk",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Install APK",
  },
  description: "Install an APK file into the Gbox Android box.",
  parameters: z
    .object({
      boxId: z.string().describe("ID of the box"),
      apk: z
        .string()
        .optional()
        .describe(
          "Local file path or HTTP(S) URL of the APK to install, for example: '/Users/jack/abc.apk', if local file provided, Gbox SDK will upload it to the box and install it. if apk is a url, Gbox SDK will download it to the box and install it(please make sure the url is public internet accessible)."
        ),
    })
    .refine((data) => data.apk, {
      message: "Either 'apk' must be provided.",
    }),
  execute: async ({ boxId, apk }: { boxId: string; apk?: string }) => {
    const box = await attachBox(boxId);
    let apkPath = apk;
    if (apk?.startsWith("file://")) {
      apkPath = apk.slice(7);
    }

    const appOperator = await box.app.install({ apk: apkPath! });
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(appOperator.data, null, 2),
        },
      ],
    };
  },
});

server.addTool({
  name: "uninstall_apk",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Uninstall APK",
  },
  description: "Uninstall an app from the Android box by package name.",
  parameters: z.object({
    boxId: z.string().describe("ID of the box"),
    packageName: z.string().describe("Android package name to uninstall"),
  }),
  execute: async ({
    boxId,
    packageName,
  }: {
    boxId: string;
    packageName: string;
  }) => {
    const box = await attachBox(boxId);
    await box.app.uninstall(packageName, {});
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify({ packageName, status: "uninstalled" }),
        },
      ],
    };
  },
});

server.addTool({
  name: "open_app",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Open App",
  },
  description:
    "Launch an installed application by package name on the Android box.",
  parameters: z.object({
    boxId: z.string().describe("ID of the box"),
    packageName: z.string().describe("Android package name to open"),
  }),
  execute: async ({
    boxId,
    packageName,
  }: {
    boxId: string;
    packageName: string;
  }) => {
    const box = await attachBox(boxId);
    const app = await box.app.get(packageName);
    await app.open();
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify({ packageName, status: "opened" }),
        },
      ],
    };
  },
});

server.addTool({
  name: "open_live_view",
  annotations: {
    openWorldHint: true,
    readOnlyHint: false,
    title: "Open Live View",
  },
  description:
    "Open the live-view URL of the Android box in the default browser.",
  parameters: z.object({
    boxId: z.string().describe("ID of the box"),
  }),
  execute: async ({ boxId }: { boxId: string }) => {
    const box = await attachBox(boxId);
    const liveView = await box.liveView();

    // Determine the appropriate command to open the URL based on the OS
    const command =
      process.platform === "darwin"
        ? `open "${liveView.url}"`
        : process.platform === "win32"
        ? `start "" "${liveView.url}"`
        : `xdg-open "${liveView.url}"`;

    exec(command, (err) => {
      if (err) {
        console.error(`Failed to open browser for URL ${liveView.url}:`, err);
      }
    });

    return {
      content: [
        {
          type: "text",
          text: `Opening live view in browser: ${liveView.url}`,
        },
      ],
    };
  },
});

server.start({
  transportType: "stdio",
});
