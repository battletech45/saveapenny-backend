package com.saveapenny.push.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FirebaseServiceAccount(
        @JsonProperty("project_id") String projectId,
        @JsonProperty("client_email") String clientEmail,
        @JsonProperty("private_key") String privateKey,
        @JsonProperty("token_uri") String tokenUri) {
}
