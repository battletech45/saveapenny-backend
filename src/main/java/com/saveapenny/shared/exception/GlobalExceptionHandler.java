package com.saveapenny.shared.exception;

import com.saveapenny.account.exception.AccountNameAlreadyExistsException;
import com.saveapenny.account.exception.AccountMutationNotAllowedException;
import com.saveapenny.account.exception.AccountNotFoundException;
import com.saveapenny.account.exception.AccountInactiveException;
import com.saveapenny.audit.exception.AuditLogAccessDeniedException;
import com.saveapenny.audit.exception.AuditLogNotFoundException;
import com.saveapenny.audit.exception.InvalidAuditDateRangeException;
import com.saveapenny.assistant.exception.AssistantChatSessionNotFoundException;
import com.saveapenny.assistant.exception.AssistantDisabledException;
import com.saveapenny.assistant.exception.AssistantProcessingException;
import com.saveapenny.automation.exception.InvalidRecurringTransactionNextRunDateException;
import com.saveapenny.automation.exception.InvalidRecurringTransactionStatusTransitionException;
import com.saveapenny.automation.exception.InvalidRecurringTransactionTypeException;
import com.saveapenny.automation.exception.RecurringTransactionDependencyNotFoundException;
import com.saveapenny.automation.exception.RecurringTransactionNotFoundException;
import com.saveapenny.notification.exception.NotificationNotFoundException;
import com.saveapenny.category.exception.CategoryNameAlreadyExistsException;
import com.saveapenny.category.exception.CategoryNotFoundException;
import com.saveapenny.category.exception.SystemCategoryModificationNotAllowedException;
import com.saveapenny.budget.exception.BudgetAlreadyExistsException;
import com.saveapenny.budget.exception.BudgetNotFoundException;
import com.saveapenny.budget.exception.InvalidBudgetDateRangeException;
import com.saveapenny.report.exception.InvalidNetWorthSnapshotDateException;
import com.saveapenny.report.exception.InvalidReportDateRangeException;
import com.saveapenny.transaction.exception.InsufficientBalanceException;
import com.saveapenny.transaction.exception.InvalidTransactionCurrencyException;
import com.saveapenny.transaction.exception.InvalidTransferException;
import com.saveapenny.transaction.exception.TransactionNotFoundException;
import com.saveapenny.auth.exception.EmailAlreadyExistsException;
import com.saveapenny.auth.exception.InvalidCredentialsException;
import com.saveapenny.auth.exception.InvalidRefreshTokenException;
import com.saveapenny.auth.exception.RefreshTokenExpiredException;
import com.saveapenny.imports.exception.ImportAlreadyRunningException;
import com.saveapenny.imports.exception.ImportNotFoundException;
import com.saveapenny.imports.exception.InvalidImportFileException;
import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.exception.GoalSimulationValidationException;
import com.saveapenny.goal.exception.InvalidGoalDateException;
import com.saveapenny.goal.exception.InvalidGoalStatusTransitionException;
import com.saveapenny.goal.exception.InvalidGoalTypeException;
import com.saveapenny.goal.exception.LinkedAccountNotFoundException;
import com.saveapenny.goal.exception.ScenarioNotFoundException;
import com.saveapenny.insight.exception.InsightGenerationException;
import com.saveapenny.insight.exception.InsightNotFoundException;
import com.saveapenny.mcp.error.ToolExecutionException;
import com.saveapenny.ocr.domain.exception.InvalidOcrFileException;
import com.saveapenny.ocr.domain.exception.OcrJobNotFoundException;
import com.saveapenny.ocr.domain.exception.OcrProcessingException;
import com.saveapenny.shared.api.ApiError;
import com.saveapenny.stock.exception.InvalidStockSymbolException;
import com.saveapenny.stock.exception.StockClientException;
import com.saveapenny.stock.exception.StockDisabledException;
import com.saveapenny.stock.exception.StockQuoteNotAvailableException;
import com.saveapenny.stock.exception.StockRateLimitExceededException;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.user.exception.InvalidPasswordException;
import com.saveapenny.user.exception.PasswordReuseNotAllowedException;
import com.saveapenny.user.exception.UserNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ToolExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleToolExecutionException(ToolExecutionException ex) {
        ApiError error = ApiError.builder()
                .code(ex.getCode().name())
                .message(ex.getMessage())
                .details(ex.getErrors().stream()
                        .map(item -> item.field() == null ? item.message() : item.field() + ": " + item.message())
                        .toList())
                .build();
        return ResponseEntity.status(resolveToolStatus(ex)).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionNotFound(TransactionNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("TRANSACTION_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    private HttpStatus resolveToolStatus(ToolExecutionException ex) {
        return switch (ex.getCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case FEATURE_DISABLED -> HttpStatus.SERVICE_UNAVAILABLE;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case TOOL_EXECUTION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransfer(InvalidTransferException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_TRANSFER")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException ex) {
        ApiError error = ApiError.builder()
                .code("INSUFFICIENT_BALANCE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidTransactionCurrencyException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransactionCurrency(InvalidTransactionCurrencyException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_TRANSACTION_CURRENCY")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCategoryNotFound(CategoryNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("CATEGORY_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(CategoryNameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleCategoryNameAlreadyExists(CategoryNameAlreadyExistsException ex) {
        ApiError error = ApiError.builder()
                .code("CATEGORY_NAME_ALREADY_EXISTS")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(SystemCategoryModificationNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSystemCategoryModificationNotAllowed(
            SystemCategoryModificationNotAllowedException ex) {
        ApiError error = ApiError.builder()
                .code("SYSTEM_CATEGORY_MODIFICATION_NOT_ALLOWED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(BudgetNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBudgetNotFound(BudgetNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("BUDGET_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(BudgetAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBudgetAlreadyExists(BudgetAlreadyExistsException ex) {
        ApiError error = ApiError.builder()
                .code("BUDGET_ALREADY_EXISTS")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidBudgetDateRangeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidBudgetDateRange(InvalidBudgetDateRangeException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_BUDGET_DATE_RANGE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidReportDateRangeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidReportDateRange(InvalidReportDateRangeException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_REPORT_DATE_RANGE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidNetWorthSnapshotDateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidNetWorthSnapshotDate(InvalidNetWorthSnapshotDateException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_NET_WORTH_SNAPSHOT_DATE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(RecurringTransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecurringTransactionNotFound(RecurringTransactionNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("RECURRING_TRANSACTION_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(RecurringTransactionDependencyNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecurringTransactionDependencyNotFound(
            RecurringTransactionDependencyNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("RECURRING_TRANSACTION_DEPENDENCY_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidRecurringTransactionNextRunDateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRecurringTransactionNextRunDate(
            InvalidRecurringTransactionNextRunDateException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_RECURRING_TRANSACTION_NEXT_RUN_DATE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidRecurringTransactionTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRecurringTransactionType(
            InvalidRecurringTransactionTypeException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_RECURRING_TRANSACTION_TYPE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidRecurringTransactionStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRecurringTransactionStatusTransition(
            InvalidRecurringTransactionStatusTransitionException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_RECURRING_TRANSACTION_STATUS_TRANSITION")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotificationNotFound(NotificationNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("NOTIFICATION_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotFound(AccountNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccountNameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNameAlreadyExists(AccountNameAlreadyExistsException ex) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_NAME_ALREADY_EXISTS")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountInactive(AccountInactiveException ex) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_INACTIVE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccountMutationNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountMutationNotAllowed(AccountMutationNotAllowedException ex) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_MUTATION_NOT_ALLOWED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(GoalNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleGoalNotFound(GoalNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("GOAL_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(ScenarioNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleScenarioNotFound(ScenarioNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("SCENARIO_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidGoalDateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidGoalDate(InvalidGoalDateException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_GOAL_DATE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidGoalStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidGoalStatusTransition(InvalidGoalStatusTransitionException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_GOAL_STATUS_TRANSITION")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidGoalTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidGoalType(InvalidGoalTypeException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_GOAL_TYPE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(LinkedAccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleLinkedAccountNotFound(LinkedAccountNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("LINKED_ACCOUNT_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(GoalSimulationValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleGoalSimulationValidation(GoalSimulationValidationException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_GOAL_SIMULATION_REQUEST")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InsightNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsightNotFound(InsightNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("INSIGHT_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InsightGenerationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsightGeneration(InsightGenerationException ex) {
        ApiError error = ApiError.builder()
                .code("INSIGHT_GENERATION_FAILED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AuditLogNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuditLogNotFound(AuditLogNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("AUDIT_LOG_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidAuditDateRangeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAuditDateRange(InvalidAuditDateRangeException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_AUDIT_DATE_RANGE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AuditLogAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuditLogAccessDenied(AuditLogAccessDeniedException ex) {
        ApiError error = ApiError.builder()
                .code("AUDIT_LOG_ACCESS_DENIED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ApiError error = ApiError.builder()
                .code("EMAIL_ALREADY_EXISTS")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_CREDENTIALS")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_REFRESH_TOKEN")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshTokenExpired(RefreshTokenExpiredException ex) {
        ApiError error = ApiError.builder()
                .code("REFRESH_TOKEN_EXPIRED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(ImportNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleImportNotFound(ImportNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("IMPORT_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidImportFileException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidImportFile(InvalidImportFileException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_IMPORT_FILE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(ImportAlreadyRunningException.class)
    public ResponseEntity<ApiResponse<Void>> handleImportAlreadyRunning(ImportAlreadyRunningException ex) {
        ApiError error = ApiError.builder()
                .code("IMPORT_ALREADY_RUNNING")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(OcrJobNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOcrJobNotFound(OcrJobNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("OCR_JOB_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidOcrFileException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOcrFile(InvalidOcrFileException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_OCR_FILE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(OcrProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleOcrProcessing(OcrProcessingException ex) {
        ApiError error = ApiError.builder()
                .code("OCR_PROCESSING_FAILED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AssistantDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAssistantDisabled(AssistantDisabledException ex) {
        ApiError error = ApiError.builder()
                .code("ASSISTANT_DISABLED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AssistantProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleAssistantProcessing(AssistantProcessingException ex) {
        ApiError error = ApiError.builder()
                .code("ASSISTANT_PROCESSING_FAILED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AssistantChatSessionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAssistantChatSessionNotFound(AssistantChatSessionNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("ASSISTANT_CHAT_SESSION_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(StockDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockDisabled(StockDisabledException ex) {
        ApiError error = ApiError.builder()
                .code("STOCK_DISABLED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidStockSymbolException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStockSymbol(InvalidStockSymbolException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_STOCK_SYMBOL")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(StockRateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockRateLimitExceeded(StockRateLimitExceededException ex) {
        ApiError error = ApiError.builder()
                .code("STOCK_RATE_LIMIT_EXCEEDED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(StockClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockClient(StockClientException ex) {
        ApiError error = ApiError.builder()
                .code("STOCK_PROVIDER_ERROR")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(StockQuoteNotAvailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockQuoteNotAvailable(StockQuoteNotAvailableException ex) {
        ApiError error = ApiError.builder()
                .code("STOCK_QUOTE_NOT_AVAILABLE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("USER_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPassword(InvalidPasswordException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_PASSWORD")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(PasswordReuseNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePasswordReuseNotAllowed(PasswordReuseNotAllowedException ex) {
        ApiError error = ApiError.builder()
                .code("PASSWORD_REUSE_NOT_ALLOWED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toMessage)
                .collect(Collectors.toList());

        ApiError error = ApiError.builder()
                .code("VALIDATION_FAILED")
                .message("Request validation failed.")
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        ApiError error = ApiError.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred.")
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ApiError error = ApiError.builder()
                .code("ACCESS_DENIED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(error));
    }

    private String toMessage(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
