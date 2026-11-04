package app.muster

import android.content.Context
import org.koin.android.ext.koin.androidContext

fun initKoin(context: Context) = startMusterKoin { androidContext(context.applicationContext) }
