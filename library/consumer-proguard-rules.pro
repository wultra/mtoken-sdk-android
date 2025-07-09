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