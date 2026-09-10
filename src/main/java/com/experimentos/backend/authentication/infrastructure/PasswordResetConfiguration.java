package com.experimentos.backend.authentication.infrastructure;

import com.experimentos.backend.authentication.application.PasswordResetProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PasswordResetProperties.class)
public class PasswordResetConfiguration {}
