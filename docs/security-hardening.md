# Security Hardening

## Tasks

### 1. Rate Limiting

- Add rate limiting to `/api/v1/auth/login` to prevent brute force attacks.
- Add general rate limiting to all API endpoints.
- Approach: bucket4j or Spring Boot filter with token bucket algorithm.
- Login: 5 requests/min per IP. General API: 60 requests/min per user.

### 2. Password Policy

- Enforce complexity: minimum 8 chars, at least one uppercase, one digit, one special character.
- Add common-password check against a known list (top 10000 passwords).
- Currently only has `@Size(min=8, max=72)` in `RegisterRequest`.
- File: `src/main/java/com/saveapenny/auth/dto/RegisterRequest.java`

### 3. Refresh Token Expiry Config

- Move hardcoded 7-day refresh token expiry to `application.yml`.
- Add `@Value("${security.jwt.refresh-token-expiry-days:7}")` in `RefreshTokenServiceImpl`.
- Currently hardcoded in `JwtServiceImpl` and `RefreshTokenServiceImpl`.

### 4. Pessimistic Locking on Balance Updates

- Add `@Lock(LockModeType.PESSIMISTIC_WRITE)` to `AccountRepository` balance queries.
- Transaction creation and transfer flows modify account balances concurrently.
- Without locking, two concurrent requests can cause lost updates.
- Files: `transaction/service/impl/TransactionServiceImpl.java`, `account/repository/AccountRepository.java`

### 5. PII Redaction in Logs

- Add Logback filter or custom converter to redact PII fields (credit card numbers, emails, full names).
- Ensure `GlobalExceptionHandler` does not log sensitive request bodies.
- Ensure `@Async` job logs do not contain raw transaction descriptions.
