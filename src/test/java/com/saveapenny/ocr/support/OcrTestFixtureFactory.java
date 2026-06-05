package com.saveapenny.ocr.support;

import com.saveapenny.ocr.application.analysis.OcrAnalysisService;
import com.saveapenny.ocr.application.analysis.classifier.OcrDocumentClassifier;
import com.saveapenny.ocr.application.extractor.DateParser;
import com.saveapenny.ocr.application.extractor.MoneyParser;
import com.saveapenny.ocr.application.extractor.OcrTextNormalizationService;
import com.saveapenny.ocr.application.parser.GenericDocumentParser;
import com.saveapenny.ocr.application.parser.OcrDocumentParser;
import com.saveapenny.ocr.application.parser.strategy.BankReceiptParser;
import com.saveapenny.ocr.application.parser.strategy.InvoiceParser;
import com.saveapenny.ocr.application.parser.strategy.RetailReceiptParser;
import com.saveapenny.ocr.application.parser.strategy.TaxReceiptParser;
import java.util.List;

public final class OcrTestFixtureFactory {

    private OcrTestFixtureFactory() {
    }

    public static OcrAnalysisService createAnalysisService() {
        OcrTextNormalizationService normalizationService = new OcrTextNormalizationService();
        DateParser dateParser = new DateParser();
        MoneyParser moneyParser = new MoneyParser();
        OcrDocumentClassifier classifier = new OcrDocumentClassifier(normalizationService);
        List<OcrDocumentParser> parsers = List.of(
                new BankReceiptParser(normalizationService, dateParser, moneyParser),
                new RetailReceiptParser(normalizationService, moneyParser),
                new TaxReceiptParser(normalizationService, moneyParser),
                new InvoiceParser(normalizationService, moneyParser),
                new GenericDocumentParser());
        return new OcrAnalysisService(normalizationService, dateParser, moneyParser, classifier, parsers);
    }
}
