# Add project specific ProGuard rules here.

# Keep NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# Keep our server classes
-keep class com.security.ravan.** { *; }
