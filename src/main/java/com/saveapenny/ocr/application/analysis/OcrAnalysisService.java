package com.saveapenny.ocr.application.analysis;

import com.saveapenny.ocr.application.analysis.classifier.OcrDocumentClassifier;
import com.saveapenny.ocr.application.extractor.DateParser;
import com.saveapenny.ocr.application.extractor.MoneyParser;
import com.saveapenny.ocr.application.extractor.OcrTextNormalizationService;
import com.saveapenny.ocr.application.parser.GenericDocumentParser;
import com.saveapenny.ocr.application.parser.OcrDocumentParser;
import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class OcrAnalysisService {

    private static final int NEARBY_LINE_WINDOW = 1;
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("(?i)(?:ref(?:erans)?\\.?\\s*no|seri[, ]*sira no)[:\\s-]*([A-Z0-9-]{5,})");
    private static final List<String> LABEL_KEYWORDS = List.of(
            "toplam", "total", "genel toplam", "tarih", "odeme tarihi", "ref no", "referans no");
    private static final List<String> CURRENCY_MARKERS = List.of("tl", "try", "usd", "eur", "gbp", "$", "€", "£");

    private final OcrTextNormalizationService normalizationService;
    private final DateParser dateParser;
    private final MoneyParser moneyParser;
    private final OcrDocumentClassifier documentClassifier;
    private final List<OcrDocumentParser> documentParsers;

    public OcrAnalysisService(
            OcrTextNormalizationService normalizationService,
            DateParser dateParser,
            MoneyParser moneyParser,
            OcrDocumentClassifier documentClassifier,
            List<OcrDocumentParser> documentParsers) {
        this.normalizationService = normalizationService;
        this.dateParser = dateParser;
        this.moneyParser = moneyParser;
        this.documentClassifier = documentClassifier;
        this.documentParsers = documentParsers;
    }

    public List<OcrTransactionCandidate> parseTransactionCandidates(String rawText) {
        return analyze(rawText).transactionCandidates();
    }

    public OcrDocumentAnalysis analyze(String rawText) {
        List<List<String>> rawBlocks = normalizationService.splitIntoNormalizedBlocks(rawText);
        if (rawBlocks.isEmpty()) {
            return new OcrDocumentAnalysis(List.of(), List.of(), null, null, null, null, null, List.of(), List.of(), List.of(), List.of(), 0.0, List.of());
        }

        List<BlockData> blocks = classifyBlocks(rawBlocks);
        LinkedHashSet<LocalDate> dates = new LinkedHashSet<>();
        LinkedHashSet<BigDecimal> amounts = new LinkedHashSet<>();
        LinkedHashSet<String> referenceNumbers = new LinkedHashSet<>();
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        List<OcrParsedField> fields = new ArrayList<>();

        for (BlockData block : blocks) {
            for (int lineIndex = 0; lineIndex < block.lines().size(); lineIndex++) {
                LineData line = block.lines().get(lineIndex);
                for (LocalDate date : line.dates()) {
                    dates.add(date);
                    fields.add(new OcrParsedField(OcrParsedFieldType.DATE, date.toString(), line.originalLine(), block.index(), lineIndex));
                }
                for (BigDecimal amount : line.amounts()) {
                    amounts.add(amount);
                    fields.add(new OcrParsedField(OcrParsedFieldType.AMOUNT, amount.toString(), line.originalLine(), block.index(), lineIndex));
                }
                for (String reference : line.references()) {
                    referenceNumbers.add(reference);
                    fields.add(new OcrParsedField(OcrParsedFieldType.REFERENCE, reference, line.originalLine(), block.index(), lineIndex));
                }
                for (String label : line.labels()) {
                    labels.add(label);
                    fields.add(new OcrParsedField(OcrParsedFieldType.LABEL, label, line.originalLine(), block.index(), lineIndex));
                }
            }
        }

        String documentType = documentClassifier.classify(rawText);
        String currency = detectCurrency(blocks);
        String merchantName = extractMerchantName(blocks);
        LocalDate paymentDate = extractLabeledDate(blocks, "odeme");
        LocalDate issueDate = extractLabeledDate(blocks, "duzenlenme", "issue");
        if (issueDate == null) {
            issueDate = extractLabeledDate(blocks, "tahakkuk");
        }
        if (merchantName != null) {
            fields.add(new OcrParsedField(OcrParsedFieldType.MERCHANT_NAME, merchantName, merchantName, 0, 0));
        }

        List<OcrDocumentBlock> documentBlocks = blocks.stream()
                .map(block -> new OcrDocumentBlock(
                        block.index(),
                        block.type(),
                        block.lines().stream().map(LineData::originalLine).toList()))
                .toList();

        List<OcrScoredCandidate> genericCandidates = scoreGenericCandidates(buildCandidates(blocks), documentType, paymentDate, currency, merchantName, labels);
        OcrDocumentAnalysisSeed seed = new OcrDocumentAnalysisSeed(
                rawText,
                documentType,
                currency,
                merchantName,
                paymentDate,
                issueDate,
                documentBlocks,
                List.copyOf(fields),
                List.copyOf(dates),
                List.copyOf(amounts),
                List.copyOf(referenceNumbers),
                List.copyOf(labels),
                genericCandidates);
        List<OcrScoredCandidate> scoredCandidates = selectParser(documentType).buildCandidates(seed);
        List<OcrTransactionCandidate> candidates = scoredCandidates.stream().map(OcrScoredCandidate::candidate).toList();
        double parseConfidence = scoredCandidates.stream()
                .mapToDouble(OcrScoredCandidate::confidence)
                .max()
                .orElse(0.0);

        return new OcrDocumentAnalysis(
                documentBlocks,
                List.copyOf(fields),
                documentType,
                currency,
                merchantName,
                paymentDate,
                issueDate,
                List.copyOf(dates),
                List.copyOf(amounts),
                List.copyOf(referenceNumbers),
                List.copyOf(labels),
                parseConfidence,
                candidates);
    }

    private List<OcrScoredCandidate> scoreGenericCandidates(
            List<OcrTransactionCandidate> candidates,
            String documentType,
            LocalDate paymentDate,
            String currency,
            String merchantName,
            LinkedHashSet<String> labels) {
        return candidates.stream()
                .map(candidate -> new OcrScoredCandidate(
                        candidate,
                        computeGenericConfidence(candidate, documentType, paymentDate, currency, merchantName, labels)))
                .toList();
    }

    private double computeGenericConfidence(
            OcrTransactionCandidate candidate,
            String documentType,
            LocalDate paymentDate,
            String currency,
            String merchantName,
            LinkedHashSet<String> labels) {
        double confidence = 0.55;
        if (paymentDate != null && paymentDate.equals(candidate.date())) {
            confidence += 0.15;
        }
        if (currency != null && !currency.isBlank()) {
            confidence += 0.05;
        }
        if (merchantName != null && !merchantName.isBlank()) {
            confidence += 0.05;
        }
        if (labels.contains("toplam") || labels.contains("total") || labels.contains("genel toplam")) {
            confidence += 0.15;
        }
        if (!"GENERIC_DOCUMENT".equals(documentType)) {
            confidence += 0.05;
        }
        return Math.min(0.89, confidence);
    }

    private OcrDocumentParser selectParser(String documentType) {
        return documentParsers.stream()
                .filter(parser -> !(parser instanceof GenericDocumentParser))
                .filter(parser -> parser.supports(documentType))
                .findFirst()
                .orElseGet(this::genericParser);
    }

    private OcrDocumentParser genericParser() {
        return documentParsers.stream()
                .filter(parser -> parser instanceof GenericDocumentParser)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Generic OCR parser strategy is not configured"));
    }

    private List<BlockData> classifyBlocks(List<List<String>> rawBlocks) {
        List<BlockData> results = new ArrayList<>();
        for (int index = 0; index < rawBlocks.size(); index++) {
            List<LineData> lines = rawBlocks.get(index).stream().map(this::toLineData).toList();
            results.add(new BlockData(index, classifyBlockType(lines, index, rawBlocks.size()), lines));
        }
        return List.copyOf(results);
    }

    private OcrDocumentBlockType classifyBlockType(List<LineData> lines, int index, int totalBlocks) {
        if (lines.stream().anyMatch(this::isFooterLine)) {
            return OcrDocumentBlockType.FOOTER;
        }
        if (lines.stream().anyMatch(this::isTotalLine)) {
            return OcrDocumentBlockType.TOTALS;
        }

        boolean hasDatesOrAmounts = lines.stream().anyMatch(line -> !line.dates().isEmpty() || !line.amounts().isEmpty());
        if (index == 0 || lines.stream().anyMatch(this::isHeaderLine)) {
            return hasDatesOrAmounts && totalBlocks == 1 ? OcrDocumentBlockType.DETAIL : OcrDocumentBlockType.HEADER;
        }
        if (hasDatesOrAmounts) {
            return OcrDocumentBlockType.DETAIL;
        }
        return OcrDocumentBlockType.OTHER;
    }

    private List<OcrTransactionCandidate> buildCandidates(List<BlockData> blocks) {
        LinkedHashMap<String, OcrTransactionCandidate> results = new LinkedHashMap<>();
        for (BlockData block : blocks) {
            for (int lineIndex = 0; lineIndex < block.lines().size(); lineIndex++) {
                OcrTransactionCandidate candidate = parseLine(blocks, block, lineIndex);
                if (candidate != null) {
                    results.putIfAbsent(candidate.date() + "|" + candidate.amount(), candidate);
                }
            }
        }

        if (!results.isEmpty()) {
            return List.copyOf(results.values());
        }

        LocalDate fallbackDate = findBestDate(blocks);
        AmountSelection fallbackAmount = findBestAmount(blocks);
        if (fallbackDate == null || fallbackAmount == null) {
            return List.of();
        }

        String description = isPreferredDescription(fallbackAmount.line(), fallbackAmount.line())
                ? fallbackAmount.line().originalLine()
                : findPreferredDescription(blocks, fallbackAmount.blockIndex());
        OcrTransactionCandidate candidate = toCandidate(fallbackDate, fallbackAmount.amount(), description);
        return candidate == null ? List.of() : List.of(candidate);
    }

    private OcrTransactionCandidate parseLine(List<BlockData> blocks, BlockData block, int index) {
        List<LineData> lines = block.lines();
        LineData current = lines.get(index);
        if (!current.dates().isEmpty() && !current.amounts().isEmpty()) {
            return toCandidate(current.dates().getFirst(), current.amounts().getLast(), current.originalLine());
        }

        if (!current.dates().isEmpty()) {
            LineData nearbyAmountLine = findNearbyLineWithAmount(lines, index);
            if (nearbyAmountLine != null) {
                return toCandidate(current.dates().getFirst(), nearbyAmountLine.amounts().getLast(), pickDescription(current, nearbyAmountLine));
            }
            AmountSelection nearbyBlockAmount = findClosestAmountInNeighborBlocks(blocks, block.index());
            if (nearbyBlockAmount != null) {
                return toCandidate(current.dates().getFirst(), nearbyBlockAmount.amount(), pickDescription(current, nearbyBlockAmount.line()));
            }
        }

        if (!current.amounts().isEmpty()) {
            LineData nearbyDateLine = findNearbyLineWithDate(lines, index);
            if (nearbyDateLine != null) {
                return toCandidate(nearbyDateLine.dates().getFirst(), current.amounts().getLast(), pickDescription(current, nearbyDateLine));
            }
            LocalDate nearbyBlockDate = findClosestDateInNeighborBlocks(blocks, block.index());
            if (nearbyBlockDate != null) {
                return toCandidate(nearbyBlockDate, current.amounts().getLast(), current.originalLine());
            }
        }

        return null;
    }

    private OcrTransactionCandidate toCandidate(LocalDate date, BigDecimal amount, String description) {
        if (date == null || amount == null) {
            return null;
        }
        String normalizedDescription = normalizationService.normalizeWhitespace(description);
        if (normalizedDescription.isBlank()) {
            return null;
        }

        return new OcrTransactionCandidate(
                date,
                amount,
                normalizedDescription,
                detectCategoryHint(normalizedDescription));
    }

    private LineData toLineData(String line) {
        String normalized = normalizationService.normalizeForMatching(line);
        return new LineData(
                line,
                normalized,
                dateParser.parseAll(line),
                moneyParser.parseAll(line),
                extractReferences(line),
                extractLabels(normalized));
    }

    private LineData findNearbyLineWithAmount(List<LineData> lines, int index) {
        for (int offset = 1; offset <= NEARBY_LINE_WINDOW; offset++) {
            LineData next = getLine(lines, index + offset);
            if (next != null && !next.amounts().isEmpty()) {
                return next;
            }
            LineData previous = getLine(lines, index - offset);
            if (previous != null && !previous.amounts().isEmpty()) {
                return previous;
            }
        }
        return null;
    }

    private LineData findNearbyLineWithDate(List<LineData> lines, int index) {
        for (int offset = 1; offset <= NEARBY_LINE_WINDOW; offset++) {
            LineData previous = getLine(lines, index - offset);
            if (previous != null && !previous.dates().isEmpty()) {
                return previous;
            }
            LineData next = getLine(lines, index + offset);
            if (next != null && !next.dates().isEmpty()) {
                return next;
            }
        }
        return null;
    }

    private LineData getLine(List<LineData> lines, int index) {
        if (index < 0 || index >= lines.size()) {
            return null;
        }
        return lines.get(index);
    }

    private String pickDescription(LineData current, LineData nearby) {
        if (containsDateLabel(current) && !nearby.amounts().isEmpty()) {
            return nearby.originalLine();
        }
        if (containsDateLabel(nearby) && !current.amounts().isEmpty()) {
            return current.originalLine();
        }
        if (isPreferredDescription(current, nearby)) {
            return current.originalLine();
        }
        if (isPreferredDescription(nearby, current)) {
            return nearby.originalLine();
        }
        return current.originalLine();
    }

    private boolean isPreferredDescription(LineData candidate, LineData other) {
        if (!candidate.originalLine().matches(".*\\p{L}.*")) {
            return false;
        }
        if (!candidate.amounts().isEmpty() && other.amounts().isEmpty()) {
            return true;
        }
        return letterCount(candidate.originalLine()) >= letterCount(other.originalLine());
    }

    private AmountSelection findBestAmount(List<BlockData> blocks) {
        return blocks.stream()
                .flatMap(block -> block.lines().stream()
                        .filter(line -> !line.amounts().isEmpty())
                        .map(line -> new AmountSelection(block.index(), block.type(), line.amounts().getLast(), line)))
                .sorted(Comparator
                        .comparing((AmountSelection selection) -> selection.type() == OcrDocumentBlockType.TOTALS ? 0 : 1)
                        .thenComparing(AmountSelection::blockIndex))
                .findFirst()
                .orElse(null);
    }

    private LocalDate findBestDate(List<BlockData> blocks) {
        for (BlockData block : blocks) {
            for (LineData line : block.lines()) {
                if (containsDateLabel(line)) {
                    return line.dates().getFirst();
                }
            }
        }
        return blocks.stream()
                .flatMap(block -> block.lines().stream())
                .filter(line -> !line.dates().isEmpty())
                .map(line -> line.dates().getFirst())
                .findFirst()
                .orElse(null);
    }

    private AmountSelection findClosestAmountInNeighborBlocks(List<BlockData> blocks, int blockIndex) {
        return blocks.stream()
                .filter(block -> block.index() != blockIndex)
                .filter(block -> Math.abs(block.index() - blockIndex) <= 1)
                .flatMap(block -> block.lines().stream()
                        .filter(line -> !line.amounts().isEmpty())
                        .map(line -> new AmountSelection(block.index(), block.type(), line.amounts().getLast(), line)))
                .sorted(Comparator.comparingInt(selection -> Math.abs(selection.blockIndex() - blockIndex)))
                .findFirst()
                .orElse(null);
    }

    private LocalDate findClosestDateInNeighborBlocks(List<BlockData> blocks, int blockIndex) {
        return blocks.stream()
                .filter(block -> block.index() != blockIndex)
                .filter(block -> Math.abs(block.index() - blockIndex) <= 1)
                .sorted(Comparator.comparingInt(block -> Math.abs(block.index() - blockIndex)))
                .flatMap(block -> block.lines().stream())
                .filter(line -> !line.dates().isEmpty())
                .map(line -> line.dates().getFirst())
                .findFirst()
                .orElse(null);
    }

    private String findPreferredDescription(List<BlockData> blocks, int preferredBlockIndex) {
        return blocks.stream()
                .sorted(Comparator.comparingInt(block -> Math.abs(block.index() - preferredBlockIndex)))
                .flatMap(block -> block.lines().stream())
                .filter(line -> line.originalLine().matches(".*\\p{L}.*"))
                .map(LineData::originalLine)
                .findFirst()
                .orElse("");
    }

    private String extractMerchantName(List<BlockData> blocks) {
        return blocks.stream()
                .filter(block -> block.type() == OcrDocumentBlockType.HEADER || block.type() == OcrDocumentBlockType.OTHER)
                .flatMap(block -> block.lines().stream())
                .filter(this::isMerchantCandidate)
                .map(LineData::originalLine)
                .findFirst()
                .orElse(null);
    }

    private String detectCurrency(List<BlockData> blocks) {
        for (BlockData block : blocks) {
            for (LineData line : block.lines()) {
                String normalized = line.normalizedLine();
                if (normalized.contains("#") && normalized.contains("tl")) {
                    return "TRY";
                }
                for (String marker : CURRENCY_MARKERS) {
                    if (!normalized.contains(marker)) {
                        continue;
                    }
                    return switch (marker) {
                        case "$", "usd" -> "USD";
                        case "€", "eur" -> "EUR";
                        case "£", "gbp" -> "GBP";
                        default -> "TRY";
                    };
                }
            }
        }
        return null;
    }

    private LocalDate extractLabeledDate(List<BlockData> blocks, String... keywords) {
        for (BlockData block : blocks) {
            for (LineData line : block.lines()) {
                if (line.dates().isEmpty()) {
                    continue;
                }
                boolean matched = false;
                for (String keyword : keywords) {
                    if (line.normalizedLine().contains(keyword)) {
                        matched = true;
                        break;
                    }
                }
                if (matched) {
                    return line.dates().getFirst();
                }
            }
        }
        return null;
    }

    private boolean isMerchantCandidate(LineData line) {
        if (!line.originalLine().matches(".*\\p{L}.*") || !line.amounts().isEmpty() || !line.references().isEmpty()) {
            return false;
        }
        if (line.normalizedLine().contains("tahsil") || line.normalizedLine().contains("receipt")) {
            return false;
        }
        return line.normalizedLine().contains("bank")
                || line.normalizedLine().contains("sube")
                || line.normalizedLine().contains("market")
                || line.normalizedLine().contains("restaurant")
                || line.normalizedLine().contains("ltd")
                || line.normalizedLine().contains("a.s")
                || letterCount(line.originalLine()) >= 10;
    }

    private boolean containsDateLabel(LineData line) {
        return !line.dates().isEmpty()
                && (line.normalizedLine().contains("tarih") || line.normalizedLine().contains("odeme"));
    }

    private boolean isFooterLine(LineData line) {
        return line.normalizedLine().contains("tesekkur")
                || line.normalizedLine().contains("kredi kart")
                || line.originalLine().contains("******");
    }

    private boolean isTotalLine(LineData line) {
        return !line.amounts().isEmpty() && (line.normalizedLine().contains("toplam") || line.normalizedLine().contains("total"));
    }

    private boolean isHeaderLine(LineData line) {
        return line.normalizedLine().contains("ref")
                || line.normalizedLine().contains("seri")
                || line.normalizedLine().contains("sube")
                || line.normalizedLine().contains("banka")
                || line.normalizedLine().contains("vergi");
    }

    private List<String> extractReferences(String line) {
        Matcher matcher = REFERENCE_PATTERN.matcher(line);
        LinkedHashSet<String> references = new LinkedHashSet<>();
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
        return List.copyOf(references);
    }

    private List<String> extractLabels(String normalizedLine) {
        return LABEL_KEYWORDS.stream()
                .filter(normalizedLine::contains)
                .toList();
    }

    private int letterCount(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private String detectCategoryHint(String line) {
        String normalized = normalizationService.normalizeForMatching(line);
        if (normalized.contains("market") || normalized.contains("grocery") || normalized.contains("restaurant")) {
            return "FOOD";
        }
        if (normalized.contains("uber") || normalized.contains("taxi") || normalized.contains("fuel")) {
            return "TRANSPORT";
        }
        if (normalized.contains("salary") || normalized.contains("payroll")) {
            return "INCOME";
        }
        return "UNCATEGORIZED";
    }

    private record LineData(
            String originalLine,
            String normalizedLine,
            List<LocalDate> dates,
            List<BigDecimal> amounts,
            List<String> references,
            List<String> labels) {
    }

    private record BlockData(
            int index,
            OcrDocumentBlockType type,
            List<LineData> lines) {
    }

    private record AmountSelection(
            int blockIndex,
            OcrDocumentBlockType type,
            BigDecimal amount,
            LineData line) {
    }
}
