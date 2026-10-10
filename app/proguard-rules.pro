# ML Kit, WorkManager and lifecycle ship their own keep rules, so nothing extra is needed yet.

# Shizuku starts ShellService by its class name in its own process, and talks to it through the AIDL stub.
-keep class io.github.teamomuito.octofiles.priv.ShellService { *; }
-keep class io.github.teamomuito.octofiles.priv.IShellService** { *; }
