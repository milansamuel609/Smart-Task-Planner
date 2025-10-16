package com.milan.smarttaskplanner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gemini.api")
@Data
public class OpenAIProperties {

    private String key;
    private String model = "gemini-2.0-flash";
    private Integer maxTokens = 2000;
    private Double temperature = 0.7;
}
