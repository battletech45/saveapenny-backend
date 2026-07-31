package com.saveapenny.feedback.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.feedback.entity.FeedbackType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedbackRequest {

    @NotNull
    private FeedbackType type;

    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    @Size(max = 5000)
    private String message;

    private JsonNode metadata;
}
