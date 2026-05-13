package com.saveapenny.report.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CashFlowPointView {

    LocalDate getDate();

    BigDecimal getIncomeAmount();

    BigDecimal getExpenseAmount();

    BigDecimal getNetAmount();
}
