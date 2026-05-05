package pt.pauloliveira.wradio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.repository.SourceConfigRepository
import javax.inject.Inject

@HiltAndroidApp
class WRadioApplication : Application() {

    @Inject
    lateinit var sourceConfigRepository: SourceConfigRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            sourceConfigRepository.refreshFromRemote()
        }
    }
}
