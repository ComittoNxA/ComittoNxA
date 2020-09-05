# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# リリース時にログ出力をオミットする
-assumenosideeffects public class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

## -keep class org.spongycastle.** { *; }

#-keep class org.spongycastle.crypto.* {*;}
#-keep class org.spongycastle.crypto.agreement.** {*;}
#-keep class org.spongycastle.crypto.digests.* {*;}
#-keep class org.spongycastle.crypto.ec. {*;}
#-keep class org.spongycastle.crypto.encodings. {*;}
#-keep class org.spongycastle.crypto.engines. {*;}
#-keep class org.spongycastle.crypto.macs. {*;}
#-keep class org.spongycastle.crypto.modes. {*;}
#-keep class org.spongycastle.crypto.paddings. {*;}
#-keep class org.spongycastle.crypto.params. {*;}
#-keep class org.spongycastle.crypto.prng. {*;}
#-keep class org.spongycastle.crypto.signers. {*;}
#-keep class org.spongycastle.pqc.crypto.mceliece.McElieceCCA2PrivateKeyParameters. {*;}
#-keep class org.spongycastle.pqc.jcajce.provider.mceliece.BCMcElieceCCA2PrivateKey.**{*;}

#-keep class org.spongycastle.jcajce.**{*;}

#-keep class org.spongycastle.jcajce.provider.asymmetric.* {*;}
#-keep class org.spongycastle.jcajce.provider.asymmetric.util. {*;}
#-keep class org.spongycastle.jcajce.provider.asymmetric.dh. {*;}
#-keep class org.spongycastle.jcajce.provider.asymmetric.ec. {*;}

#-keep class org.spongycastle.jcajce.provider.digest.** {*;}
#-keep class org.spongycastle.jcajce.provider.keystore.** {*;}
-keep class org.spongycastle.jcajce.provider.symmetric.** {*;}
#-keep class org.bouncycastle.jcajce.provider.symmetric.** {*;}
