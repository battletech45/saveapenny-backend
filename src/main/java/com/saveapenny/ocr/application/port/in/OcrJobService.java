package com.saveapenny.ocr.application.port.in;

import com.saveapenny.ocr.interfaces.http.dto.OcrJobStatusResponse;
import com.saveapenny.ocr.interfaces.http.dto.OcrSubmitResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface OcrJobService {

    OcrSubmitResponse createJob(UUID currentUserId, MultipartFile file);

    OcrJobStatusResponse getJobStatus(UUID currentUserId, UUID jobId);
}
