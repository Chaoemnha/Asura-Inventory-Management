package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityReport {
    private Long salesCreated;
    private Long salesCompleted;
    private Long purchasesCreated;
    private Long purchasesCompleted;
    private Long totalProductsSaleCreated;
    private Long totalProductsSaleCompleted;
    private Long totalProductsPurchaseCreated;
    private Long totalProductsPurchaseCompleted;
}
