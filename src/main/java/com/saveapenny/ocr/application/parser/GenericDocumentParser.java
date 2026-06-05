package com.saveapenny.ocr.application.parser;

import com.saveapenny.ocr.application.analysis.OcrDocumentAnalysisSeed;
import com.saveapenny.ocr.application.analysis.OcrScoredCandidate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GenericDocumentParser implements OcrDocumentParser {

    @Override
    public boolean supports(String documentType) {
        return true;
    }

    @Override
    public List<OcrScoredCandidate> buildCandidates(OcrDocumentAnalysisSeed seed) {
        return seed.genericCandidates();
    }
}
