# Add project-specific Proguard rules here.
# By default, the flags in this file are appended to flags specified
# in proguard-android-optimize.txt.

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room compiler generated classes
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.**
