# RGRemote uses platform networking, Room, and Android Keystore.
# Keep enums serialized to DB/Prefs by name
-keepclassmembers enum com.rgremote.app.domain.DeviceType { *; }
-keepclassmembers enum com.rgremote.app.domain.AppTargetSource { *; }
-keepclassmembers enum com.rgremote.app.domain.HdmiPort { *; }

# Keep Room entities and generated DAOs
-keep class com.rgremote.app.data.db.** { *; }

