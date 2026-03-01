# Enum class
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class top.secret.hma.v1.data.UpdateData { *; }
-keep class top.secret.hma.v1.data.UpdateData$* { *; }

-keep,allowoptimization class * extends androidx.preference.PreferenceFragmentCompat
-keepclassmembers class top.secret.hma.v1.databinding.**  {
    public <methods>;
}
