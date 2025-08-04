# Android Tester MCP

A Model Context Protocol (MCP) server that provides seamless integration between AI assistants (like Claude Code) and Android Studio for automated Android application control. This project enables programmatic control of Android app lifecycle through natural language commands.

## 🚀 Features

- **App Lifecycle Control**: Start, stop, rerun, and debug Android applications
- **Configuration Management**: List and select Android run configurations
- **Real-time Status**: Check API server status and app execution state
- **Natural Language Interface**: Control Android apps through AI assistant commands
- **Cross-platform**: Works with any Android Studio project
- **HTTP API Integration**: Communicates with Gbox Android Studio Plugin

## 📋 Prerequisites

Before using this MCP server, ensure you have:

1. **Node.js** (v18 or higher) and npm
2. **Android Studio** with the Gbox Android Studio Plugin installed
3. **Gbox plugin API Server** running on port 8765

### Installing the Gbox Android Studio Plugin

1. Clone the plugin repository:
   ```bash
   git clone <plugin-repo-url>
   cd gbox-android-studio-plugin
   ```

2. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```

3. Install the generated plugin from `build/distributions/` in Android Studio:
   - Go to **File → Settings → Plugins**
   - Click **Install Plugin from Disk**
   - Select the generated `.zip` file

4. Start the API server:
   - In Android Studio, go to **Tools → Gbox → Start API Server**
   - The server will run on `http://localhost:8765`

## 🛠️ Installation & Setup

### 1. Install Dependencies

```bash
npm install
```

### 2. Build the Project

```bash
npm run build
```

### 3. Configure MCP in Cursor

Add the following configuration to your Cursor settings:

```json
{
  "mcpServers": {
    "gbox-rerun": {
      "command": "node",
      "args": ["/path/to/your/android-tester-mcp/dist/index.js"]
    }
  }
}
```

**Note**: Replace `/path/to/your/android-tester-mcp` with the actual path to your project directory.

## 🎯 Available Tools

This MCP server provides the following tools for Android application control:

| Tool | Description | Parameters |
|------|-------------|------------|
| `android_start_app` | Start the Android application | `projectPath` (optional) |
| `android_stop_app` | Stop the currently running Android application | `projectPath` (optional) |
| `android_rerun_app` | Rerun the Android application (stop and start) | `projectPath` (optional) |
| `android_debug_app` | Start debugging the Android application | `projectPath` (optional) |
| `android_get_configurations` | Get list of available Android run configurations | `projectPath` (optional) |
| `android_select_configuration` | Select a specific Android run configuration | `configurationName`, `projectPath` (optional) |
| `android_api_status` | Check the status of the Android Studio plugin API server | None |

## 💡 Usage Examples

### Basic App Control

```
> Start the Android app in debug mode
> Stop the running Android application
> Rerun the app with the "debug" configuration
```

### Configuration Management

```
> Show me all available run configurations
> Select the "app" configuration and start debugging
> List all Android run configurations for the current project
```

### Status and Diagnostics

```
> Check if the Android API server is running
> Verify the connection to Android Studio
> Get the current app status
```

## 🔧 Development

### Project Structure

```
android-tester-mcp/
├── src/
│   └── index.ts          # Main MCP server implementation
├── gbox-android-studio-plugin/  # Android Studio plugin
│   ├── src/main/kotlin/  # Plugin source code
│   └── build.gradle.kts  # Build configuration
├── package.json          # Node.js dependencies
└── tsconfig.json         # TypeScript configuration
```

### Available Scripts

```bash
npm run build     # Build the TypeScript project
npm run start     # Start the MCP server
npm run dev       # Run in development mode
npm run inspect   # Run MCP inspector for debugging
```

### Development Workflow

1. **Start the API server** in Android Studio (Tools → Gbox → Start API Server)
2. **Build the MCP server**: `npm run build`
3. **Test with MCP inspector**: `npm run inspect`
4. **Configure in Cursor** and test with natural language commands

## 🔍 Troubleshooting

### Common Issues

**"API request failed: ECONNREFUSED"**
- Ensure the Gbox plugin API server is running in Android Studio
- Check that the server is running on port 8765
- Verify the plugin is properly installed

**"Failed to get configurations"**
- Make sure an Android project is open in Android Studio
- Check that the project has run configurations defined
- Verify the project path is correct

**"Error starting Android app"**
- Ensure an Android device or emulator is connected
- Check that the selected run configuration is valid
- Verify the project builds successfully

### Debug Mode

Run the MCP inspector to debug tool calls:

```bash
npm run inspect
```

This will start an interactive session where you can test individual tools and see detailed responses.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests if applicable
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Related Projects

- **Gbox Android Studio Plugin**: The companion Android Studio plugin that provides the HTTP API
- **Model Context Protocol**: The protocol specification for AI tool integration
- **FastMCP**: The MCP framework used in this project

## 📞 Support

- **Issues**: Create an issue on GitHub for bugs or feature requests
- **Discussions**: Use GitHub Discussions for questions and community support
- **Documentation**: Check the [Gbox Android Studio Plugin README](gbox-android-studio-plugin/README.md) for detailed plugin documentation

---

**Made with ❤️ for the Android development community**
