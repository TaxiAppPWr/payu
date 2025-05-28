package com.example.payu.configuration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payu")
public class PayuProperties {
    private String customerIp;
    private String merchantPosId;
    private String continueUrl;
    private String description;

}
