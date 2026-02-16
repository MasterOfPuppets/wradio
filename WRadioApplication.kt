package pt.pauloliveira.wradio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// @HiltAndroidApp triggers Hilt's code generation.
@HiltAndroidApp
class WRadioApplication : Application()