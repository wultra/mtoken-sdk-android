# there's usage of GSON's @SerializedName
-keepattributes *Annotation*

-keepclasseswithmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

-keep, allowobfuscation class com.wultra.android.mtokensdk.api.** { *; }