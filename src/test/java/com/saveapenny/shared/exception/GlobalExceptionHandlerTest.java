package com.saveapenny.shared.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.saveapenny.account.exception.AccountInactiveException;
import com.saveapenny.account.exception.AccountNameAlreadyExistsException;
import com.saveapenny.account.exception.AccountNotFoundException;
import com.saveapenny.assistant.exception.AssistantChatSessionNotFoundException;
import com.saveapenny.assistant.exception.AssistantDisabledException;
import com.saveapenny.assistant.exception.AssistantProcessingException;
import com.saveapenny.audit.exception.AuditLogAccessDeniedException;
import com.saveapenny.audit.exception.AuditLogNotFoundException;
import com.saveapenny.audit.exception.InvalidAuditDateRangeException;
import com.saveapenny.automation.exception.InvalidRecurringTransactionNextRunDateException;
import com.saveapenny.automation.exception.InvalidRecurringTransactionTypeException;
import com.saveapenny.automation.exception.RecurringTransactionDependencyNotFoundException;
import com.saveapenny.automation.exception.RecurringTransactionNotFoundException;
import com.saveapenny.budget.exception.BudgetAlreadyExistsException;
import com.saveapenny.budget.exception.BudgetNotFoundException;
import com.saveapenny.budget.exception.InvalidBudgetDateRangeException;
import com.saveapenny.category.exception.CategoryNameAlreadyExistsException;
import com.saveapenny.category.exception.CategoryNotFoundException;
import com.saveapenny.category.exception.SystemCategoryModificationNotAllowedException;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.exception.GoalSimulationValidationException;
import com.saveapenny.goal.exception.InvalidGoalDateException;
import com.saveapenny.goal.exception.InvalidGoalStatusTransitionException;
import com.saveapenny.goal.exception.InvalidGoalTypeException;
import com.saveapenny.goal.exception.LinkedAccountNotFoundException;
import com.saveapenny.goal.exception.ScenarioNotFoundException;
import com.saveapenny.imports.exception.ImportAlreadyRunningException;
import com.saveapenny.imports.exception.ImportNotFoundException;
import com.saveapenny.imports.exception.InvalidImportFileException;
import com.saveapenny.insight.exception.InsightGenerationException;
import com.saveapenny.insight.exception.InsightNotFoundException;
import com.saveapenny.mcp.error.ToolError;
import com.saveapenny.mcp.error.ToolErrorCode;
import com.saveapenny.mcp.error.ToolExecutionException;
import com.saveapenny.notification.exception.NotificationNotFoundException;
import com.saveapenny.ocr.domain.exception.InvalidOcrFileException;
import com.saveapenny.ocr.domain.exception.OcrJobNotFoundException;
import com.saveapenny.ocr.domain.exception.OcrProcessingException;
import com.saveapenny.report.exception.InvalidNetWorthSnapshotDateException;
import com.saveapenny.report.exception.InvalidReportDateRangeException;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.exception.InsufficientBalanceException;
import com.saveapenny.transaction.exception.InvalidTransferException;
import com.saveapenny.transaction.exception.TransactionNotFoundException;
import com.saveapenny.user.exception.InvalidPasswordException;
import com.saveapenny.user.exception.PasswordReuseNotAllowedException;
import com.saveapenny.user.exception.UserNotFoundException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleTransactionNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleTransactionNotFound(new TransactionNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND");
    }

    @Test
    void handleInvalidTransfer_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidTransfer(new InvalidTransferException("invalid"));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_TRANSFER");
    }

    @Test
    void handleInsufficientBalance_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInsufficientBalance(new InsufficientBalanceException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE");
    }

    @Test
    void handleCategoryNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleCategoryNotFound(new CategoryNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND");
    }

    @Test
    void handleCategoryNameAlreadyExists_returnsConflict() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleCategoryNameAlreadyExists(new CategoryNameAlreadyExistsException("name"));
        assertStatusAndCode(response, HttpStatus.CONFLICT, "CATEGORY_NAME_ALREADY_EXISTS");
    }

    @Test
    void handleSystemCategoryModificationNotAllowed_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleSystemCategoryModificationNotAllowed(
                        new SystemCategoryModificationNotAllowedException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "SYSTEM_CATEGORY_MODIFICATION_NOT_ALLOWED");
    }

    @Test
    void handleBudgetNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBudgetNotFound(new BudgetNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "BUDGET_NOT_FOUND");
    }

    @Test
    void handleBudgetAlreadyExists_returnsConflict() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBudgetAlreadyExists(new BudgetAlreadyExistsException());
        assertStatusAndCode(response, HttpStatus.CONFLICT, "BUDGET_ALREADY_EXISTS");
    }

    @Test
    void handleInvalidBudgetDateRange_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidBudgetDateRange(new InvalidBudgetDateRangeException(LocalDate.now(), LocalDate.now()));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_BUDGET_DATE_RANGE");
    }

    @Test
    void handleInvalidReportDateRange_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidReportDateRange(new InvalidReportDateRangeException(LocalDate.now(), LocalDate.now()));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_REPORT_DATE_RANGE");
    }

    @Test
    void handleInvalidNetWorthSnapshotDate_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidNetWorthSnapshotDate(new InvalidNetWorthSnapshotDateException(LocalDate.now()));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_NET_WORTH_SNAPSHOT_DATE");
    }

    @Test
    void handleRecurringTransactionNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleRecurringTransactionNotFound(new RecurringTransactionNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "RECURRING_TRANSACTION_NOT_FOUND");
    }

    @Test
    void handleRecurringTransactionDependencyNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleRecurringTransactionDependencyNotFound(
                        new RecurringTransactionDependencyNotFoundException("accountId", UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "RECURRING_TRANSACTION_DEPENDENCY_NOT_FOUND");
    }

    @Test
    void handleInvalidRecurringTransactionNextRunDate_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidRecurringTransactionNextRunDate(
                        new InvalidRecurringTransactionNextRunDateException(LocalDate.now()));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_RECURRING_TRANSACTION_NEXT_RUN_DATE");
    }

    @Test
    void handleInvalidRecurringTransactionType_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidRecurringTransactionType(
                        new InvalidRecurringTransactionTypeException(TransactionType.INCOME));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_RECURRING_TRANSACTION_TYPE");
    }

    @Test
    void handleNotificationNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleNotificationNotFound(new NotificationNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND");
    }

    @Test
    void handleAccountNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccountNotFound(new AccountNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
    }

    @Test
    void handleAccountNameAlreadyExists_returnsConflict() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccountNameAlreadyExists(new AccountNameAlreadyExistsException("name"));
        assertStatusAndCode(response, HttpStatus.CONFLICT, "ACCOUNT_NAME_ALREADY_EXISTS");
    }

    @Test
    void handleAccountInactive_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccountInactive(new AccountInactiveException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "ACCOUNT_INACTIVE");
    }

    @Test
    void handleGoalNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGoalNotFound(new GoalNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "GOAL_NOT_FOUND");
    }

    @Test
    void handleScenarioNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleScenarioNotFound(new ScenarioNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "SCENARIO_NOT_FOUND");
    }

    @Test
    void handleInvalidGoalDate_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidGoalDate(new InvalidGoalDateException(LocalDate.now()));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_GOAL_DATE");
    }

    @Test
    void handleInvalidGoalStatusTransition_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidGoalStatusTransition(
                        new InvalidGoalStatusTransitionException(UUID.randomUUID(), null, null));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_GOAL_STATUS_TRANSITION");
    }

    @Test
    void handleInvalidGoalType_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidGoalType(new InvalidGoalTypeException(GoalType.SAVINGS, "SAVINGS"));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_GOAL_TYPE");
    }

    @Test
    void handleLinkedAccountNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleLinkedAccountNotFound(new LinkedAccountNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "LINKED_ACCOUNT_NOT_FOUND");
    }

    @Test
    void handleGoalSimulationValidation_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGoalSimulationValidation(new GoalSimulationValidationException("invalid"));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_GOAL_SIMULATION_REQUEST");
    }

    @Test
    void handleInsightNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInsightNotFound(new InsightNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "INSIGHT_NOT_FOUND");
    }

    @Test
    void handleInsightGeneration_returnsInternalServerError() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInsightGeneration(new InsightGenerationException("gen failed"));
        assertStatusAndCode(response, HttpStatus.INTERNAL_SERVER_ERROR, "INSIGHT_GENERATION_FAILED");
    }

    @Test
    void handleAuditLogNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAuditLogNotFound(new AuditLogNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "AUDIT_LOG_NOT_FOUND");
    }

    @Test
    void handleInvalidAuditDateRange_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidAuditDateRange(
                        new InvalidAuditDateRangeException(OffsetDateTime.now(), OffsetDateTime.now()));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_AUDIT_DATE_RANGE");
    }

    @Test
    void handleAuditLogAccessDenied_returnsForbidden() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAuditLogAccessDenied(new AuditLogAccessDeniedException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.FORBIDDEN, "AUDIT_LOG_ACCESS_DENIED");
    }

    @Test
    void handleImportNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleImportNotFound(new ImportNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "IMPORT_NOT_FOUND");
    }

    @Test
    void handleInvalidImportFile_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidImportFile(new InvalidImportFileException("bad file"));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_IMPORT_FILE");
    }

    @Test
    void handleImportAlreadyRunning_returnsConflict() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleImportAlreadyRunning(new ImportAlreadyRunningException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.CONFLICT, "IMPORT_ALREADY_RUNNING");
    }

    @Test
    void handleOcrJobNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleOcrJobNotFound(new OcrJobNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "OCR_JOB_NOT_FOUND");
    }

    @Test
    void handleInvalidOcrFile_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidOcrFile(new InvalidOcrFileException("bad file"));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_OCR_FILE");
    }

    @Test
    void handleOcrProcessing_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleOcrProcessing(new OcrProcessingException("processing error"));
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "OCR_PROCESSING_FAILED");
    }

    @Test
    void handleAssistantDisabled_returnsServiceUnavailable() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAssistantDisabled(new AssistantDisabledException());
        assertStatusAndCode(response, HttpStatus.SERVICE_UNAVAILABLE, "ASSISTANT_DISABLED");
    }

    @Test
    void handleAssistantProcessing_returnsBadGateway() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAssistantProcessing(
                        new AssistantProcessingException("processing error", new RuntimeException("cause")));
        assertStatusAndCode(response, HttpStatus.BAD_GATEWAY, "ASSISTANT_PROCESSING_FAILED");
    }

    @Test
    void handleAssistantChatSessionNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAssistantChatSessionNotFound(new AssistantChatSessionNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "ASSISTANT_CHAT_SESSION_NOT_FOUND");
    }

    @Test
    void handleUserNotFound_returnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUserNotFound(new UserNotFoundException(UUID.randomUUID()));
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }

    @Test
    void handleInvalidPassword_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidPassword(new InvalidPasswordException());
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "INVALID_PASSWORD");
    }

    @Test
    void handlePasswordReuseNotAllowed_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handlePasswordReuseNotAllowed(new PasswordReuseNotAllowedException());
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "PASSWORD_REUSE_NOT_ALLOWED");
    }

    @Test
    void handleToolExecution_withUnauthorizedCode_returnsUnauthorized() {
        ToolExecutionException ex = new ToolExecutionException(ToolErrorCode.UNAUTHORIZED, "unauthorized");
        ResponseEntity<ApiResponse<Void>> response = handler.handleToolExecutionException(ex);
        assertStatusAndCode(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    @Test
    void handleToolExecution_withNotFoundCode_returnsNotFound() {
        ToolExecutionException ex = new ToolExecutionException(ToolErrorCode.NOT_FOUND, "not found");
        ResponseEntity<ApiResponse<Void>> response = handler.handleToolExecutionException(ex);
        assertStatusAndCode(response, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    @Test
    void handleToolExecution_withValidationError_returnsBadRequest() {
        ToolExecutionException ex = new ToolExecutionException(ToolErrorCode.VALIDATION_ERROR, "validation error");
        ResponseEntity<ApiResponse<Void>> response = handler.handleToolExecutionException(ex);
        assertStatusAndCode(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }

    @Test
    void handleToolExecution_withFeatureDisabled_returnsServiceUnavailable() {
        ToolExecutionException ex = new ToolExecutionException(ToolErrorCode.FEATURE_DISABLED, "disabled");
        ResponseEntity<ApiResponse<Void>> response = handler.handleToolExecutionException(ex);
        assertStatusAndCode(response, HttpStatus.SERVICE_UNAVAILABLE, "FEATURE_DISABLED");
    }

    @Test
    void handleToolExecution_withRateLimited_returnsTooManyRequests() {
        ToolExecutionException ex = new ToolExecutionException(ToolErrorCode.RATE_LIMITED, "rate limited");
        ResponseEntity<ApiResponse<Void>> response = handler.handleToolExecutionException(ex);
        assertStatusAndCode(response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED");
    }

    @Test
    void handleToolExecution_withExecutionFailed_returnsInternalServerError() {
        ToolExecutionException ex = new ToolExecutionException(ToolErrorCode.TOOL_EXECUTION_FAILED, "execution failed");
        ResponseEntity<ApiResponse<Void>> response = handler.handleToolExecutionException(ex);
        assertStatusAndCode(response, HttpStatus.INTERNAL_SERVER_ERROR, "TOOL_EXECUTION_FAILED");
    }

    @Test
    void handleToolExecution_includesErrorDetails() {
        List<ToolError> errors = List.of(
                new ToolError(ToolErrorCode.VALIDATION_ERROR, "field error", "field1"));
        ToolExecutionException ex = new ToolExecutionException(ToolErrorCode.VALIDATION_ERROR, "validation error", errors);
        ResponseEntity<ApiResponse<Void>> response = handler.handleToolExecutionException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().getError().getCode());
        assertEquals(1, response.getBody().getError().getDetails().size());
        assertEquals("field1: field error", response.getBody().getError().getDetails().get(0));
    }

    @Test
    void handleValidation_returnsBadRequest() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
        bindingResult.addError(new FieldError("test", "field1", "must not be null"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_FAILED", response.getBody().getError().getCode());
        assertNotNull(response.getBody().getError().getDetails());
    }

    @Test
    void handleGeneric_returnsInternalServerError() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGeneric(new RuntimeException("unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError().getCode());
        assertEquals("An unexpected error occurred.", response.getBody().getError().getMessage());
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDenied(new AccessDeniedException("access denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("ACCESS_DENIED", response.getBody().getError().getCode());
    }

    private void assertStatusAndCode(ResponseEntity<ApiResponse<Void>> response, HttpStatus expectedStatus, String expectedCode) {
        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getError());
        assertEquals(expectedCode, response.getBody().getError().getCode());
    }
}
