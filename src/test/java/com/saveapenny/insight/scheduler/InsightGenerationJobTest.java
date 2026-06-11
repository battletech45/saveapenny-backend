package com.saveapenny.insight.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.insight.config.InsightProperties;
import com.saveapenny.insight.service.impl.InsightGenerationPipeline;
import com.saveapenny.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class InsightGenerationJobTest {

    @Mock
    private InsightProperties insightProperties;
    @Mock
    private InsightGenerationPipeline insightGenerationPipeline;
    @Mock
    private UserRepository userRepository;

    private InsightGenerationJob job;

    @BeforeEach
    void setUp() {
        job = new InsightGenerationJob(insightProperties, insightGenerationPipeline, userRepository);
    }

    @Test
    void generateInsightsForAllUsers_whenDisabled_doesNothing() {
        when(insightProperties.enabled()).thenReturn(false);

        job.generateInsightsForAllUsers();

        verify(userRepository, never()).findAllUserIds();
        verify(insightGenerationPipeline, never()).execute(any());
    }

    @Test
    void generateInsightsForAllUsers_processesAllUsers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        when(insightProperties.enabled()).thenReturn(true);
        when(userRepository.findAllUserIds(PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 100), 2));
        when(insightGenerationPipeline.execute(user1)).thenReturn(5);
        when(insightGenerationPipeline.execute(user2)).thenReturn(3);

        job.generateInsightsForAllUsers();

        verify(insightGenerationPipeline).execute(user1);
        verify(insightGenerationPipeline).execute(user2);
    }

    @Test
    void generateInsightsForAllUsers_pipelineException_continuesToNextUser() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        when(insightProperties.enabled()).thenReturn(true);
        when(userRepository.findAllUserIds(PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 100), 2));
        when(insightGenerationPipeline.execute(user1)).thenThrow(new RuntimeException("pipeline error"));
        when(insightGenerationPipeline.execute(user2)).thenReturn(2);

        job.generateInsightsForAllUsers();

        verify(insightGenerationPipeline).execute(user1);
        verify(insightGenerationPipeline).execute(user2);
    }

    @Test
    void generateInsightsForAllUsers_noUsers_doesNothing() {
        when(insightProperties.enabled()).thenReturn(true);
        when(userRepository.findAllUserIds(PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        job.generateInsightsForAllUsers();

        verify(insightGenerationPipeline, never()).execute(any());
    }

    @Test
    void generateInsightsForAllUsers_paginatesThroughUsers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        when(insightProperties.enabled()).thenReturn(true);
        when(userRepository.findAllUserIds(PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(user1), PageRequest.of(0, 100), 101));
        when(userRepository.findAllUserIds(PageRequest.of(1, 100)))
                .thenReturn(new PageImpl<>(List.of(user2), PageRequest.of(1, 100), 101));
        when(insightGenerationPipeline.execute(any())).thenReturn(1);

        job.generateInsightsForAllUsers();

        verify(insightGenerationPipeline).execute(user1);
        verify(insightGenerationPipeline).execute(user2);
    }
}
