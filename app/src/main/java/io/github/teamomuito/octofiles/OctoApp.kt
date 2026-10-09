package io.github.teamomuito.octofiles

import android.app.Application
import io.github.teamomuito.octofiles.data.Prefs
import io.github.teamomuito.octofiles.work.Jobs
import io.github.teamomuito.octofiles.work.Notifier

class OctoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Notifier.createChannel(this)
        Jobs.ensureScheduled(this)
    }
}
