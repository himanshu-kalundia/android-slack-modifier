# Keep the Notification Listener Service
-keep class com.hnkapps.slanotif.SlackNotificationListener { *; }

# Keep Material Design components
-keep class com.google.android.material.** { *; }

# Keep AndroidX components
-keep class androidx.** { *; }

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
