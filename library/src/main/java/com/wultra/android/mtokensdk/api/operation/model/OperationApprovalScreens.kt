package com.wultra.android.mtokensdk.api.operation.model

open class PreApprovalScreen(
        /**
         * type of PreApprovalScreen is presented with different classes (Starting with `WMTPreApprovalScreen*`)
         */
        val type: PreApprovalScreenType) {

    enum class PreApprovalScreenType(val value: String) {
        INFO("INFO"),
        WARNING("WARNING"),
        QR("QR_SCAN")
    }
}

class PreApprovalScreenInfo(
    val heading: String,
    val message: String,
    val items: List<String>,
    val approvalType: PreApprovalScreenConfirmAction
) : PreApprovalScreen(type = PreApprovalScreenType.INFO) {

}

class PreApprovalScreenWarning(
    val heading: String,
    val message: String,
    val items: List<String>,
    val approvalType: PreApprovalScreenConfirmAction
) : PreApprovalScreen(type = PreApprovalScreenType.WARNING)  {

}

enum class PreApprovalScreenConfirmAction {
    SLIDER
}

open class PostApprovalScreen(
    /**
     * type of PostApprovalScreen is presented with different classes (Starting with `WMTPreApprovalScreen*`)
     */
    val type: PostApprovalScreenType) {

    enum class PostApprovalScreenType(val value: String) {
        REVIEW("REVIEW"),
        REDIRECT("MERCHANT_REDIRECT"),
        GENERIC("GENERIC"),
    }
}

class PostApprovalScreenReview(
    val heading: String,
    val message: String,
    val payload: ReviewPostApprovalScreenPayload
) : PostApprovalScreen(type = PostApprovalScreenType.REVIEW)

class PostApprovalScreenRedirect(
    val heading: String,
    val message: String,
    val payload: RedirectPostApprovalScreenPayload
) : PostApprovalScreen(type = PostApprovalScreenType.REDIRECT)

class PostApprovalScreenGeneric(
    val heading: String,
    val message: String,
    val payload: GenericPostApprovalScreenPayload
) : PostApprovalScreen(type = PostApprovalScreenType.GENERIC)

open class PostApprovalScreenPayload

class RedirectPostApprovalScreenPayload(
    val text: String,
    val url: String,
    val countdown: String
): PostApprovalScreenPayload()

class ReviewPostApprovalScreenPayload(
    val attributes: Array<ReviewAttributes>
): PostApprovalScreenPayload() {
    class ReviewAttributes(
        val type: String,
        val id: String,
        val label: String,
        val note: String
    )
}

class GenericPostApprovalScreenPayload: PostApprovalScreenPayload()
