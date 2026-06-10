package com.saveapenny.insight.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.insight.config.InsightProperties;
import com.saveapenny.insight.service.impl.InsightGenerationPipeline;
import com.saveapenny.user.entity.User;
import com.saveapenny.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        verify(userRepository, never()).findAll();
        verify(insightGenerationPipeline, never()).execute(any());
    }

    @Test
    void generateInsightsForAllUsers_processesAllUsers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        List<User> users = List.of(
                User.builder().id(user1).build(),
                User.builder().id(user2).build());

        when(insightProperties.enabled()).thenReturn(true);
        when(userRepository.findAll()).thenReturn(users);
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
        List<User> users = List.of(
                User.builder().id(user1).build(),
                User.builder().id(user2).build());

        when(insightProperties.enabled()).thenReturn(true);
        when(userRepository.findAll()).thenReturn(users);
        when(insightGenerationPipeline.execute(user1)).thenThrow(new RuntimeException("pipeline error"));
        when(insightGenerationPipeline.execute(user2)).thenReturn(2);

        job.generateInsightsForAllUsers();

        verify(insightGenerationPipeline).execute(user1);
        verify(insightGenerationPipeline).execute(user2);
    }

    @Test
    void generateInsightsForAllUsers_noUsers_doesNothing() {
        when(insightProperties.enabled()).thenReturn(true);
        when(userRepository.findAll()).thenReturn(List.of());

        job.generateInsightsForAllUsers();

        verify(insightGenerationPipeline, never()).execute(any());
    }
}
