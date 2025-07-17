/*
 * BLS日志记录器
 * 用于将应用程序日志推送到百度云BLS服务
 */
package com.megadotnet.logging.ext;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bls.BlsClient;
import com.baidubce.services.bls.BlsClientConfiguration;
import com.baidubce.services.bls.model.logrecord.PushLogRecordRequest;
import com.baidubce.services.bls.model.logrecord.LogRecord;
import com.baidubce.services.bls.model.logrecord.LogType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * BLS日志记录器配置类
 * 负责初始化BLS客户端并处理日志记录的推送
 */
public class BLSLogbackAppender extends AppenderBase<ILoggingEvent> {
    private BlsClient client;
    private String endpoint;
    private String projectName;
    private String logstoreName;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化BLS客户端配置
     * 从系统属性和环境变量中获取配置信息
     */
    @Override
    public void start() {
        // 从系统属性获取BLS配置
        this.endpoint = System.getProperty("logging.bls.endpoint", "bls-log.bj.baidubce.com");
        this.logstoreName = System.getProperty("logging.bls.logstore", "oauth-app");

        // 从环境变量获取BCE凭证
        Map<String, String> env = System.getenv();
        String ak = env.getOrDefault("BAIDU_BCE_AK", "default-ak");
        String sk = env.getOrDefault("BAIDU_BCE_SK", "default-sk");

        this.projectName = System.getProperty("spring.application.name", "default");

        BlsClientConfiguration config = new BlsClientConfiguration();
        config.setEndpoint(endpoint);
        config.setCredentials(new DefaultBceCredentials(ak, sk));

        this.client = new BlsClient(config);
        super.start();
    }

    /**
     * 处理并发送日志记录到BLS服务
     * @param event 日志事件对象
     */
    @Override
    protected void append(ILoggingEvent event) {
        try {
            /* 创建日志记录并构建JSON格式日志内容 */
            LogRecord logRecord = new LogRecord();
            logRecord.setTimestamp(System.currentTimeMillis());
            
            // 构建JSON格式日志内容
            String jsonContent = objectMapper.writeValueAsString(new LogContent(
                event.getLevel().toString(),
                event.getFormattedMessage(),
                event.getLoggerName()
            ));
            
            logRecord.setMessage(jsonContent);
            
            /* 构建日志推送请求并发送日志 */
            PushLogRecordRequest request = new PushLogRecordRequest(
                this.projectName,
                this.logstoreName,
                null, // logStreamName
                LogType.JSON,
                Arrays.asList(logRecord),
                null // logTags
            );
            
            // 发送日志
            client.pushLogRecord(request);
        } catch (Exception e) {
            addError("Failed to push log record", e);
        }
    }
    
    /*
     * 日志内容数据传输对象
     * 用于将日志信息序列化为JSON格式
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class LogContent {
        @JsonProperty("level")
        private final String level;
        @JsonProperty("message")
        private final String message;
        @JsonProperty("logger")
        private final String logger;

        /**
         * 构造日志内容对象
         * @param level 日志级别
         * @param message 格式化后的日志消息
         * @param logger 发出日志的记录器名称
         */
        public LogContent(String level, String message, String logger) {
            this.level = level;
            this.message = message;
            this.logger = logger;
        }

        /**
         * 获取日志级别
         * @return 日志级别字符串
         */
        public String getLevel() { return level; }
        
        /**
         * 获取日志消息
         * @return 格式化后的日志消息
         */
        public String getMessage() { return message; }
        
        /**
         * 获取记录器名称
         * @return 发出日志的记录器名称
         */
        public String getLogger() { return logger; }
    }
}