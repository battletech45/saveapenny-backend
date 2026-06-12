package com.saveapenny.insight.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.insight.analytics.InsightCandidate;
import com.saveapenny.insight.config.InsightProperties;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiEnhancementService {

    private static final Logger log = LoggerFactory.getLogger(AiEnhancementService.class);
    private static final TypeReference<List<EnhancedInsightContent>> ENHANCED_CONTENT_LIST = new TypeReference<>() {};
    private static final String SYSTEM_PROMPT = """
            Rewrite machine-generated financial insights into concise, supportive user-facing copy.
            Return only valid JSON as an array of objects with exactly these fields:
            [{"title":"...","summary":"...","detail":"..."}]
            Rules:
            - Preserve the number of items and their order.
            - Do not change facts, amounts, dates, percentages, currencies, or trend direction.
            - Keep titles short and specific.
            - Keep summaries concise and actionable.
            - Keep detail factual and compact, or return null if no detail is needed.
            - No markdown, no code fences, no commentary.
            """;

    private final InsightProperties insightProperties;
    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectMapper objectMapper;

    public AiEnhancementService(
            InsightProperties insightProperties,
            @Qualifier("insightChatClient") ObjectProvider<ChatClient> chatClientProvider,
            ObjectMapper objectMapper) {
        this.insightProperties = insightProperties;
        this.chatClientProvider = chatClientProvider;
        this.objectMapper = objectMapper;
    }

    public List<InsightCandidate> enhance(List<InsightCandidate> candidates) {
        if (!insightProperties.aiEnhanced() || candidates.isEmpty()) {
            return candidates;
        }

        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            log.warn("Insight AI enhancement is enabled, but no insight chat client is configured; returning raw candidates");
            return candidates;
        }

        try {
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(buildUserPrompt(candidates))
                    .call()
                    .content();
            return mergeEnhancedContent(candidates, response);
        } catch (Exception ex) {
            log.warn("Insight AI enhancement failed; returning raw candidates", ex);
            return candidates;
        }
    }

    private String buildUserPrompt(List<InsightCandidate> candidates) throws Exception {
        return objectMapper.writeValueAsString(candidates.stream()
                .map(candidate -> new InsightRewriteRequest(
                        candidate.type().name(),
                        candidate.title(),
                        candidate.summary(),
                        candidate.detail(),
                        candidate.severity(),
                        candidate.metadata()))
                .toList());
    }

    private List<InsightCandidate> mergeEnhancedContent(List<InsightCandidate> candidates, String response) throws Exception {
        if (!StringUtils.hasText(response)) {
            log.warn("Insight AI enhancement returned an empty response; returning raw candidates");
            return candidates;
        }

        List<EnhancedInsightContent> enhancedContent = objectMapper.readValue(stripMarkdownCodeFence(response), ENHANCED_CONTENT_LIST);
        if (enhancedContent.size() != candidates.size()) {
            log.warn(
                    "Insight AI enhancement returned {} items for {} candidates; returning raw candidates",
                    enhancedContent.size(),
                    candidates.size());
            return candidates;
        }

        List<InsightCandidate> result = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            InsightCandidate original = candidates.get(i);
            EnhancedInsightContent enhanced = enhancedContent.get(i);
            result.add(new InsightCandidate(
                    original.type(),
                    coalesce(enhanced.title(), original.title()),
                    coalesce(enhanced.summary(), original.summary()),
                    coalesce(enhanced.detail(), original.detail()),
                    original.categoryId(),
                    original.severity(),
                    original.metadata()));
        }

        return result;
    }

    private String stripMarkdownCodeFence(String response) {
        String trimmed = response.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) {
            return trimmed;
        }

        String withoutOpeningFence = trimmed.substring(firstNewline + 1);
        int closingFence = withoutOpeningFence.lastIndexOf("```");
        if (closingFence < 0) {
            return withoutOpeningFence.trim();
        }

        return withoutOpeningFence.substring(0, closingFence).trim();
    }

    private String coalesce(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : fallback;
    }

    private record InsightRewriteRequest(
            String type,
            String title,
            String summary,
            String detail,
            String severity,
            String metadata) {}

    private record EnhancedInsightContent(
            String title,
            String summary,
            String detail) {}
}
