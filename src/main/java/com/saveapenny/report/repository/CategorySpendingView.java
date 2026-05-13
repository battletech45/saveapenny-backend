package com.saveapenny.report.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface CategorySpendingView {

    UUID getCategoryId();

    String getCategoryName();

    BigDecimal getTotalAmount();
}
