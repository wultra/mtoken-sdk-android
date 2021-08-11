package com.wultra.android.mtokensdk.api.operation.model

/**
 * Object returned from the operation history endpoint.
 */
data class OperationHistoryEntry(
    /** Processing status of the operation */
    val status: OperationHistoryEntryStatus,
    /** Operation */
    val operation: UserOperation
)

/** Processing status of the operation */
enum class OperationHistoryEntryStatus {
    /** Operation was approved */
    APPROVED,
    /** Operation was rejected */
    REJECTED,
    /** Operation is pending its resolution */
    PENDING,
    /** Operation was canceled */
    CANCELED,
    /** Operation expired */
    EXPIRED,
    /** Operation failed */
    FAILED
}
