# BLSLogback

一个用于集成百度云BLS（百度日志服务）的自定义Logback附加器，实现集中化日志管理和分析。

## 概述

本项目提供了Logback日志框架与百度云BLS之间的无缝集成，允许Java应用程序自动将结构化JSON日志推送到百度云日志服务。

## 功能特性

- **自定义Logback附加器**：扩展Logback的`AppenderBase`，直接将日志推送到BLS
- **JSON日志格式**：自动将日志事件转换为结构化JSON格式
- **灵活配置**：支持通过系统属性和环境变量进行配置
- **错误处理**：优雅的错误处理机制，具备降级日志记录功能
- **依赖排除管理**：排除冲突的日志依赖项（log4j、hbase-client）

## 依赖要求

- **Java 1.8+**
- **Logback 1.2.11**：核心日志框架
- **百度BCE Java SDK 0.10.380**：用于BLS集成
- **Jackson 2.13.3**：JSON处理

## 配置说明

### 环境变量

设置以下环境变量用于BCE凭证：

```bash
export BAIDU_BCE_AK=你的访问密钥
export BAIDU_BCE_SK=你的秘密密钥
```

### 系统属性

通过系统属性配置BLS设置：

```bash
-Dlogging.bls.endpoint=bls-log.bj.baidubce.com
-Dlogging.bls.logstore=你的日志存储名称
-Dspring.application.name=你的应用名称
```

### Logback配置

项目包含一个`logback.xml`配置文件，设置了：

- 带格式化时间戳的控制台输出
- 用于云日志记录的BLS附加器
- 包特定的日志级别
- 根日志记录器配置

## 使用方法

1. **添加依赖**：将此项目作为依赖项包含在你的Maven项目中
2. **设置凭证**：将BCE访问密钥和秘密密钥配置为环境变量
3. **配置属性**：为BLS端点和日志存储设置系统属性
4. **使用标准日志记录**：在应用程序中使用标准的SLF4J/Logback日志记录

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YourApplication {
    private static final Logger logger = LoggerFactory.getLogger(YourApplication.class);
    
    public void someMethod() {
        logger.info("此日志将同时发送到控制台和BLS");
        logger.error("错误日志也会自动推送到BLS");
    }
}
```

## JSON日志结构

日志会自动转换为以下结构的JSON格式：

```json
{
    "level": "INFO",
    "message": "你的日志消息",
    "logger": "com.example.YourClass"
}
```

## 构建和运行

```bash
# 构建项目
mvn clean compile

# 带配置运行
java -Dlogging.bls.endpoint=bls-log.bj.baidubce.com \
     -Dlogging.bls.logstore=你的日志存储 \
     -Dspring.application.name=你的应用 \
     -cp target/classes:target/dependency/* \
     com.megadotnet.YourMainClass
```

## 项目结构

```
src/
├── main/
│   └── java/
│       └── com/megadotnet/logging/ext/
│           └── BLSLogbackAppender.java
└── test/
    └── (测试文件)
```

## 核心组件

- **BLSLogbackAppender**：处理BLS集成的自定义Logback附加器
- **LogContent**：JSON日志结构的内部类
- **配置管理**：从环境变量和系统属性自动配置

## 默认值

- **端点**：`bls-log.bj.baidubce.com`
- **日志存储**：`oauth-app`
- **项目名称**：`default`
- **访问密钥**：`default-ak`（应该被覆盖）
- **秘密密钥**：`default-sk`（应该被覆盖）

## 许可证

本项目是megadotnet组织日志扩展的一部分。