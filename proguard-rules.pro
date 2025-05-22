-keep public class com.strikesdialogues.StrikesDialogues extends org.bukkit.plugin.java.JavaPlugin {
    public void onEnable();
    public void onDisable();
}

-keep public class * implements org.bukkit.command.CommandExecutor
-keep public class * implements org.bukkit.command.TabCompleter

-keep public class * implements org.bukkit.event.Listener
-keepclasseswithmembers 'public class * { @org.bukkit.event.EventHandler *; }'

-keep class org.bukkit.** { *; }
-keep class net.kyori.adventure.** { *; }
-keep class com.comphenix.protocol.** { *; }
-keep class me.clip.placeholderapi.** { *; }

-keep @org.bukkit.event.EventHandler class *
-keepclassmembers class * {
    @org.bukkit.event.EventHandler *;
}
-keepattributes *Annotation*

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 3
-allowaccessmodification
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile