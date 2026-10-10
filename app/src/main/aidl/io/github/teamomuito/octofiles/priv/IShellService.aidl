package io.github.teamomuito.octofiles.priv;

// Runs in Shizuku's shell process. See ShellService.kt.
interface IShellService {
    void destroy() = 16777114; // Shizuku calls this to stop the service
    String exec(String command) = 1;
}
