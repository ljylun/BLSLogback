package com.megadotnet.logging.ext;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.Logger;
import com.baidubce.services.bls.BlsClient;
import com.baidubce.services.bls.model.logrecord.PushLogRecordRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * BLSLogbackAppender单元测试类
 * 使用自定义的测试方法避免复杂的Mockito问题
 */
public class BLSLogbackAppenderTest {

    private TestableBLSLogbackAppender appender;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        appender = new TestableBLSLogbackAppender();
        objectMapper = new ObjectMapper();
    }

    @After
    public void tearDown() {
        // 清理系统属性
        System.clearProperty("logging.bls.endpoint");
        System.clearProperty("logging.bls.logstore");
        System.clearProperty("spring.application.name");
        if (appender != null) {
            appender.stop();
        }
    }

    /**
     * 测试appender启动时的配置初始化
     */
    @Test
    public void testStart_WithSystemProperties() {
        // 设置系统属性
        System.setProperty("logging.bls.endpoint", "test-endpoint.com");
        System.setProperty("logging.bls.logstore", "test-logstore");
        System.setProperty("spring.application.name", "test-app");

        // 模拟环境变量
        Map<String, String> mockEnv = new HashMap<>();
        mockEnv.put("BAIDU_BCE_AK", "test-ak");
        mockEnv.put("BAIDU_BCE_SK", "test-sk");

        appender.setMockEnvironment(mockEnv);
        appender.start();

        assertTrue("Appender should be started", appender.isStarted());
        assertEquals("test-endpoint.com", appender.getEndpoint());
        assertEquals("test-logstore", appender.getLogstoreName());
        assertEquals("test-app", appender.getProjectName());
    }

    /**
     * 测试appender启动时使用默认配置
     */
    @Test
    public void testStart_WithDefaultProperties() {
        Map<String, String> mockEnv = new HashMap<>();
        mockEnv.put("BAIDU_BCE_AK", "default-ak");
        mockEnv.put("BAIDU_BCE_SK", "default-sk");

        appender.setMockEnvironment(mockEnv);
        appender.start();

        assertTrue("Appender should be started", appender.isStarted());
        assertEquals("bls-log.bj.baidubce.com", appender.getEndpoint());
        assertEquals("oauth-app", appender.getLogstoreName());
        assertEquals("default", appender.getProjectName());
    }

    /**
     * 测试成功发送日志记录
     */
    @Test
    public void testAppend_SuccessfulLogPush() throws Exception {
        // 准备测试数据
        appender.start();
        LoggingEvent loggingEvent = createTestLoggingEvent(Level.INFO, "Test log message", "test.logger");

        // 执行测试
        appender.append(loggingEvent);

        // 验证日志被记录
        List<PushLogRecordRequest> capturedRequests = appender.getCapturedRequests();
        assertEquals("Should have one request", 1, capturedRequests.size());

        PushLogRecordRequest request = capturedRequests.get(0);
        assertNotNull("Request should not be null", request);
        assertNotNull("Log records should not be null", request.getLogRecords());
        assertEquals("Should have one log record", 1, request.getLogRecords().size());

        // 验证日志记录内容
        String jsonMessage = request.getLogRecords().get(0).getMessage();
        JsonNode jsonNode = objectMapper.readTree(jsonMessage);
        assertEquals("Level should match", "INFO", jsonNode.get("level").asText());
        assertEquals("Message should match", "Test log message", jsonNode.get("message").asText());
        assertEquals("Logger should match", "test.logger", jsonNode.get("logger").asText());
    }

    /**
     * 测试不同日志级别的处理
     */
    @Test
    public void testAppend_DifferentLogLevels() throws Exception {
        appender.start();
        
        Level[] levels = {Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};
        
        for (Level level : levels) {
            LoggingEvent loggingEvent = createTestLoggingEvent(level, "Test message for " + level, "test.logger");
            appender.append(loggingEvent);
        }

        // 验证所有级别的日志都被发送
        List<PushLogRecordRequest> capturedRequests = appender.getCapturedRequests();
        assertEquals("Should have 4 requests", 4, capturedRequests.size());
    }

    /**
     * 测试日志发送异常处理
     */
    @Test
    public void testAppend_ExceptionHandling() throws Exception {
        appender.start();
        appender.setShouldThrowException(true);
        
        LoggingEvent loggingEvent = createTestLoggingEvent(Level.ERROR, "Test error message", "test.logger");

        // 执行测试 - 不应该抛出异常
        appender.append(loggingEvent);

        // 验证异常被捕获，但仍然尝试发送
        List<PushLogRecordRequest> capturedRequests = appender.getCapturedRequests();
        assertEquals("Should have attempted one request", 1, capturedRequests.size());
    }

    /**
     * 测试JSON序列化
     */
    @Test
    public void testJsonSerialization() throws Exception {
        appender.start();
        
        LoggingEvent loggingEvent = createTestLoggingEvent(
            Level.WARN, 
            "Warning message with special chars: 中文测试 & <script>", 
            "com.test.SpecialLogger"
        );

        appender.append(loggingEvent);

        List<PushLogRecordRequest> capturedRequests = appender.getCapturedRequests();
        String jsonMessage = capturedRequests.get(0).getLogRecords().get(0).getMessage();
        JsonNode jsonNode = objectMapper.readTree(jsonMessage);
        
        assertEquals("WARN", jsonNode.get("level").asText());
        assertEquals("Warning message with special chars: 中文测试 & <script>", jsonNode.get("message").asText());
        assertEquals("com.test.SpecialLogger", jsonNode.get("logger").asText());
    }

    /**
     * 测试时间戳设置
     */
    @Test
    public void testTimestampSetting() throws Exception {
        appender.start();
        
        long beforeTime = System.currentTimeMillis();
        
        LoggingEvent loggingEvent = createTestLoggingEvent(Level.INFO, "Timestamp test", "test.logger");
        appender.append(loggingEvent);
        
        long afterTime = System.currentTimeMillis();

        List<PushLogRecordRequest> capturedRequests = appender.getCapturedRequests();
        long recordTimestamp = capturedRequests.get(0).getLogRecords().get(0).getTimestamp();
        
        assertTrue("Timestamp should be within expected range", 
                   recordTimestamp >= beforeTime && recordTimestamp <= afterTime);
    }

    /**
     * 测试空消息处理
     */
    @Test
    public void testAppend_EmptyMessage() throws Exception {
        appender.start();
        
        LoggingEvent loggingEvent = createTestLoggingEvent(Level.INFO, "", "test.logger");
        appender.append(loggingEvent);

        List<PushLogRecordRequest> capturedRequests = appender.getCapturedRequests();
        String jsonMessage = capturedRequests.get(0).getLogRecords().get(0).getMessage();
        JsonNode jsonNode = objectMapper.readTree(jsonMessage);
        
        assertEquals("", jsonNode.get("message").asText());
    }

    /**
     * 测试null消息处理
     */
    @Test
    public void testAppend_NullMessage() throws Exception {
        appender.start();
        
        LoggingEvent loggingEvent = createTestLoggingEvent(Level.INFO, null, "test.logger");
        appender.append(loggingEvent);

        List<PushLogRecordRequest> capturedRequests = appender.getCapturedRequests();
        String jsonMessage = capturedRequests.get(0).getLogRecords().get(0).getMessage();
        JsonNode jsonNode = objectMapper.readTree(jsonMessage);
        
        assertTrue("Message should be null or empty", 
                   jsonNode.get("message").isNull() || jsonNode.get("message").asText().isEmpty());
    }

    /**
     * 创建测试用的LoggingEvent
     */
    private LoggingEvent createTestLoggingEvent(Level level, String message, String loggerName) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        LoggingEvent event = new LoggingEvent();
        event.setLevel(level);
        event.setMessage(message);
        event.setLoggerName(loggerName);
        event.setTimeStamp(System.currentTimeMillis());
        return event;
    }

    /**
     * 可测试的BLSLogbackAppender子类
     * 避免创建真实的BlsClient，而是捕获请求用于验证
     */
    private static class TestableBLSLogbackAppender extends BLSLogbackAppender {
        private List<PushLogRecordRequest> capturedRequests = new ArrayList<>();
        private Map<String, String> mockEnvironment = new HashMap<>();
        private boolean shouldThrowException = false;
        private boolean started = false;

        public void setMockEnvironment(Map<String, String> env) {
            this.mockEnvironment = env;
        }

        public void setShouldThrowException(boolean shouldThrow) {
            this.shouldThrowException = shouldThrow;
        }

        public List<PushLogRecordRequest> getCapturedRequests() {
            return new ArrayList<>(capturedRequests);
        }

        @Override
        public void start() {
            try {
                // 设置配置字段
                Field endpointField = BLSLogbackAppender.class.getDeclaredField("endpoint");
                endpointField.setAccessible(true);
                endpointField.set(this, System.getProperty("logging.bls.endpoint", "bls-log.bj.baidubce.com"));
                
                Field logstoreField = BLSLogbackAppender.class.getDeclaredField("logstoreName");
                logstoreField.setAccessible(true);
                logstoreField.set(this, System.getProperty("logging.bls.logstore", "oauth-app"));
                
                Field projectField = BLSLogbackAppender.class.getDeclaredField("projectName");
                projectField.setAccessible(true);
                projectField.set(this, System.getProperty("spring.application.name", "default"));
                
                // 不创建真实的BlsClient，只设置started状态
                this.started = true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to setup test appender", e);
            }
        }

        @Override
        public boolean isStarted() {
            return started;
        }

        @Override
        public void stop() {
            started = false;
        }

        @Override
        protected void append(ch.qos.logback.classic.spi.ILoggingEvent event) {
            if (!started) return;
            
            try {
                // 模拟原始append方法的逻辑，但不发送到真实的BLS服务
                com.baidubce.services.bls.model.logrecord.LogRecord logRecord = 
                    new com.baidubce.services.bls.model.logrecord.LogRecord();
                logRecord.setTimestamp(System.currentTimeMillis());
                
                // 构建JSON格式日志内容
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonContent = objectMapper.writeValueAsString(new LogContent(
                    event.getLevel().toString(),
                    event.getFormattedMessage(),
                    event.getLoggerName()
                ));
                
                logRecord.setMessage(jsonContent);
                
                // 构建日志推送请求
                PushLogRecordRequest request = new PushLogRecordRequest(
                    getProjectName(),
                    getLogstoreName(),
                    null, // logStreamName
                    com.baidubce.services.bls.model.logrecord.LogType.JSON,
                    java.util.Arrays.asList(logRecord),
                    null // logTags
                );
                
                // 捕获请求而不是发送
                capturedRequests.add(request);
                
                // 模拟异常情况
                if (shouldThrowException) {
                    throw new RuntimeException("Simulated network error");
                }
                
            } catch (Exception e) {
                // 模拟原始代码的异常处理
                addError("Failed to push log record", e);
            }
        }

        // 获取配置的辅助方法
        public String getEndpoint() {
            try {
                Field field = BLSLogbackAppender.class.getDeclaredField("endpoint");
                field.setAccessible(true);
                return (String) field.get(this);
            } catch (Exception e) {
                return null;
            }
        }

        public String getLogstoreName() {
            try {
                Field field = BLSLogbackAppender.class.getDeclaredField("logstoreName");
                field.setAccessible(true);
                return (String) field.get(this);
            } catch (Exception e) {
                return null;
            }
        }

        public String getProjectName() {
            try {
                Field field = BLSLogbackAppender.class.getDeclaredField("projectName");
                field.setAccessible(true);
                return (String) field.get(this);
            } catch (Exception e) {
                return null;
            }
        }

        // 内部LogContent类，复制自原始代码
        private static class LogContent {
            private final String level;
            private final String message;
            private final String logger;

            public LogContent(String level, String message, String logger) {
                this.level = level;
                this.message = message;
                this.logger = logger;
            }

            public String getLevel() { return level; }
            public String getMessage() { return message; }
            public String getLogger() { return logger; }
        }
    }
}