# youtubedl-android ships no consumer rules. It loads Python/ffmpeg via JNI and
# maps yt-dlp JSON with Jackson reflection, so keep it whole.
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Apache Commons Compress (used by youtubedl-android to unpack Python/ffmpeg at first launch).
# ExtraFieldUtils instantiates its ZipExtraField classes by reflection; without this R8 makes
# them abstract and app start crashes with "class ... is not a concrete class".
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn org.objectweb.asm.**
