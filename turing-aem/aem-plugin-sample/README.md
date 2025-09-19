# Turing AEM Plugin Sample

This is a sample implementation of the Turing AEM Connector Plugin that demonstrates how to create custom extensions for Adobe Experience Manager (AEM) content indexing and search integration with Viglet Turing.

## Overview

The AEM Plugin Sample provides a working example of how to:
- Extend AEM content indexing capabilities
- Implement custom content processors for AEM fragments
- Configure delta indexing for incremental content updates
- Set up AEM-to-Turing integration with the WKND sample site

## What This Sample Includes

### Core Components

- **Custom Extensions**: Sample implementations of AEM extension interfaces
  - `TurAemExtSampleDeltaDate`: Handles delta date processing for incremental indexing
  - `TurAemExtSampleModelJson`: Processes AEM content fragments and model JSON data

- **Configuration**: Pre-configured setup for the WKND sample site
  - Source configuration with AEM author/publish endpoints
  - Attribute mappings for content indexing
  - Locale-specific path configurations

- **Scripts**: Automated build and deployment scripts
  - Build automation with Maven and Ant
  - Runtime configuration for different environments
  - Ready-to-run connector setup

### Sample Data Model

The sample includes a `TurAemSampleModel` bean that demonstrates how to handle AEM content fragments with custom properties like `fragmentPath`.

## Prerequisites

Before deploying this sample, ensure you have:

1. **Java 21+** ☕
2. **Maven 3.6+** 📦
3. **Apache Ant** (for build scripts) 🐜
4. **Adobe Experience Manager 6.5+** running locally or remotely
5. **Viglet Turing** server running and accessible
6. **AEM WKND Sample Site** (recommended for testing)

## Quick Start

### 1. Clone and Build

```bash
# Navigate to the sample directory
cd turing-aem/aem-plugin-sample

# Build the sample using the provided script
compile-and-run.cmd
```

This will:
- Clean and compile the Maven project
- Package the sample plugin
- Copy required JARs to the distribution directory
- Launch the connector

### 2. Manual Build Process

If you prefer to build manually:

```bash
# Build the sample plugin
./mvnw clean install package

# Build using Ant (creates dist/wknd directory)
ant wknd

# Navigate to the distribution directory
cd dist/wknd

# Run the connector
run.cmd
```

## Configuration

### Environment Configuration

Edit `scripts/wknd/env.cmd` to configure your environment:

```batch
set TURING_URL=http://localhost:2700
set TURING_API_KEY=your_api_key_here
```

### AEM Source Configuration

The sample includes a pre-configured WKND source in `scripts/wknd/export/wknd.json`:

```json
{
  "sources": [
    {
      "name": "WKND",
      "endpoint": "http://localhost:4502",
      "username": "admin",
      "password": "admin",
      "rootPath": "/content/wknd",
      "contentType": "cq:Page",
      "authorSNSite": "wknd-author",
      "publishSNSite": "wknd-publish"
    }
  ]
}
```

### Key Configuration Parameters

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `endpoint` | AEM instance URL | `http://localhost:4502` |
| `username` | AEM admin username | `admin` |
| `password` | AEM admin password | `admin` |
| `rootPath` | Content root path to index | `/content/wknd` |
| `authorURLPrefix` | Author instance URL prefix | `http://localhost:4502` |
| `publishURLPrefix` | Publish instance URL prefix | `https://wknd.site` |

## Deployment

### Option 1: Using Build Scripts

1. **Configure environment variables** in `scripts/wknd/env.cmd`
2. **Run the automated build and deployment**:
   ```cmd
   compile-and-run.cmd
   ```

### Option 2: Manual Deployment

1. **Build the project**:
   ```bash
   ./mvnw clean install package
   ```

2. **Create distribution directory**:
   ```bash
   mkdir -p dist/wknd/libs
   ```

3. **Copy required files**:
   ```bash
   # Copy sample plugin
   cp target/aem-plugin-sample.jar dist/wknd/libs/
   
   # Copy AEM plugin
   cp ../aem-plugin/target/aem-plugin.jar dist/wknd/libs/
   
   # Copy connector application
   cp ../../turing-connector/connector-app/target/turing-connector.jar dist/wknd/
   
   # Copy scripts
   cp -r scripts/wknd/* dist/wknd/
   ```

4. **Run the connector**:
   ```bash
   cd dist/wknd
   java -Dloader.path=libs \
        -Dturing.url=http://localhost:2700 \
        -Dturing.apiKey=your_api_key \
        -Dspring.h2.console.enabled=true \
        -jar turing-connector.jar
   ```

## Usage

### Starting the Connector

Once deployed, start the connector with:

```cmd
cd dist/wknd
run.cmd
```

The connector will:
1. Load the sample plugin extensions
2. Connect to your AEM instance
3. Connect to Turing server
4. Start indexing content from the configured root path

### Custom Extensions

To create your own extensions:

1. **Implement the required interfaces**:
   - `TurAemExtContentInterface` for content processing
   - `TurAemExtDeltaDateInterface` for delta indexing

2. **Add your extension classes to the configuration**:
   ```json
   {
     "deltaClass": "com.your.package.YourDeltaExtension",
     "attributes": [
       {
         "className": "com.your.package.YourContentExtension"
       }
     ]
   }
   ```

## Troubleshooting

### Common Issues

**Q: Connection refused to AEM**
A: Verify AEM is running and accessible at the configured endpoint. Check username/password.

**Q: Turing API key invalid**
A: Ensure your API key is correct in `env.cmd` and that Turing server is running.

**Q: No content being indexed**
A: Check that the `rootPath` exists in AEM and contains the specified `contentType`.

**Q: Build fails**
A: Ensure Java 21+ is installed and `JAVA_HOME` is set correctly.

### Debugging

Enable debug logging by adding to your run command:
```bash
-Dlogging.level.com.viglet.turing=DEBUG
```

### Support

- 📋 [GitHub Discussions](https://github.com/openviglet/turing/discussions)
- 🐛 [Report Issues](https://github.com/openviglet/turing/issues)
- 📖 [Main Documentation](../../README.adoc)

## Next Steps

1. **Explore the code**: Review the extension implementations in `src/main/java`
2. **Customize configuration**: Modify `wknd.json` for your specific AEM setup
3. **Create custom extensions**: Implement your own content processors
4. **Test with real content**: Replace WKND with your actual AEM content structure

This sample provides a foundation for building production-ready AEM-to-Turing integrations. Customize the extensions and configuration to match your specific content models and indexing requirements.
