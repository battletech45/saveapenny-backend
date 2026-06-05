package com.saveapenny.ocr.application.parser;

import com.saveapenny.ocr.application.analysis.OcrDocumentAnalysisSeed;
import com.saveapenny.ocr.application.analysis.OcrScoredCandidate;
import java.util.List;

public interface OcrDocumentParser {

    boolean supports(String documentType);

    List<OcrScoredCandidate> buildCandidates(OcrDocumentAnalysisSeed seed);
}
