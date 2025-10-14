# there's usage of GSON's @SerializedName
-keepattributes *Annotation*

-keepclasseswithmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# handle Gson
-keepclassmembers class com.wultra.android.mtokensdk.api.** {
     <fields>;
}
# handle R8 full mode optimizations
-keep, allowobfuscation class com.wultra.android.mtokensdk.api.**

# Keep the enum class and its field names unobfuscated
-keepnames class com.wultra.android.mtokensdk.api.operation.model.mobiletokendata.PreApprovalScreensRecorder$ScreenCloseAction { *; }