# BLSLogback

A custom Logback appender for integrating with Baidu Cloud BLS (Baidu Log Service) to enable centralized log management and analysis.

## Overview

This project provides a seamless integration between Logback logging framework and Baidu Cloud BLS, allowing Java applications to automatically push structured JSON logs to Baidu's cloud logging service.

## Features

- **Custom Logback Appender**: Extends Logback's `AppenderBase` to push logs directly to BLS
- **JSON Log Format**: Automatically converts log events to structured JSON format
- **Flexible Configuration**: Supports configuration via system properties and environment variables
- **Error Handling**: Graceful error handling with fallback logging
- **Exclusion Management**: Excludes conflicting logging dependencies (log4j, hbase-client)

## Dependencies

- **Java 1.8+**
- **Logback 1.2.11**: Core logging framework
- **Baidu BCE Java SDK 0.10.380**: For BLS integration
- **Jackson 2.13.3**: JSON processing

## Configuration

### Environment Variables

Set the following environment variables for BCE credentials:

```bash
export BAIDU_BCE_AK=your-access-key
export BAIDU_BCE_SK=your-secret-key
```

### System Properties

Configure BLS settings via system properties:

```bash
-Dlogging.bls.endpoint=bls-log.bj.baidubce.com
-Dlogging.bls.logstore=your-logstore-name
-Dspring.application.name=your-app-name
```

### Logback Configuration

The project includes a `logback.xml` configuration file that sets up:

- Console output with formatted timestamps
- BLS appender for cloud logging
- Package-specific log levels
- Root logger configuration

## Usage

1. **Add Dependencies**: Include this project as a dependency in your Maven project
2. **Set Credentials**: Configure BCE access key and secret key as environment variables
3. **Configure Properties**: Set system properties for BLS endpoint and logstore
4. **Use Standard Logging**: Use standard SLF4J/Logback logging in your application

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YourApplication {
    private static final Logger logger = LoggerFactory.getLogger(YourApplication.class);
    
    public void someMethod() {
        logger.info("This log will be sent to both console and BLS");
        logger.error("Error logs are also automatically pushed to BLS");
    }
}
```

## JSON Log Structure

Logs are automatically converted to JSON format with the following structure:

```json
{
    "level": "INFO",
    "message": "Your log message",
    "logger": "com.example.YourClass"
}
```

## Build and Run

```bash
# Build the project
mvn clean compile

# Run with configuration
java -Dlogging.bls.endpoint=bls-log.bj.baidubce.com \
     -Dlogging.bls.logstore=your-logstore \
     -Dspring.application.name=your-app \
     -cp target/classes:target/dependency/* \
     com.megadotnet.YourMainClass
```

## Project Structure

```
src/
├── main/
│   └── java/
│       └── com/megadotnet/logging/ext/
│           └── BLSLogbackAppender.java
└── test/
    └── (test files)
```

## Key Components

- **BLSLogbackAppender**: Custom Logback appender that handles BLS integration
- **LogContent**: Internal class for JSON log structure
- **Configuration Management**: Automatic configuration from environment and system properties

## Default Values

- **Endpoint**: `bls-log.bj.baidubce.com`
- **Logstore**: `oauth-app`
- **Project Name**: `default`
- **Access Key**: `default-ak` (should be overridden)
- **Secret Key**: `default-sk` (should be overridden)

## License

This project is part of the megadotnet organization's logging extensions.