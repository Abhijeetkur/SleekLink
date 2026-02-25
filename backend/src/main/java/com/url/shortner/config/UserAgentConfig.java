package com.url.shortner.config;

import com.blueconic.browscap.BrowsCapField;
import com.blueconic.browscap.UserAgentParser;
import com.blueconic.browscap.UserAgentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class UserAgentConfig {

    @Bean
    public UserAgentParser userAgentParser() throws Exception {
        return new UserAgentService().loadParser(Arrays.asList(
                BrowsCapField.BROWSER,
                BrowsCapField.BROWSER_TYPE,
                BrowsCapField.BROWSER_MAJOR_VERSION,
                BrowsCapField.DEVICE_TYPE,
                BrowsCapField.PLATFORM,
                BrowsCapField.PLATFORM_VERSION));
    }
}
