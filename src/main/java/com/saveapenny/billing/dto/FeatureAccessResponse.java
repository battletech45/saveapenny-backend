package com.saveapenny.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureAccessResponse {

    private boolean assistant;
    private boolean insights;
    private boolean stocks;
    private boolean ocr;
    private boolean csvImport;
    private boolean reportExport;
    private boolean advancedRecurring;
    private boolean goalWhatIf;
}
