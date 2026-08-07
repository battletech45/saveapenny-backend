package com.saveapenny.feedback.dto;

import com.saveapenny.feedback.entity.FeedbackStatus;
import jakarta.validation.constraints.NotNull;
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
public class UpdateFeedbackStatusRequest {

    @NotNull
    private FeedbackStatus status;
}
