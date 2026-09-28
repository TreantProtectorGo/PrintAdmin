package com.ethan.printadmin.dto;

public record UserUsageResponse(Long userId, int monthlyQuota, long usedPages, long remainingPages) {
}
