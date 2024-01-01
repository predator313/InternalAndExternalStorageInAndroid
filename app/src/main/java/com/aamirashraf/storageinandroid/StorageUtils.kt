package com.aamirashraf.storageinandroid

import android.os.Build

//when we use generic we basically used the inline function
inline fun <T> sdk29OrUP(onSdk29:()->T):T?{
    return if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
        onSdk29()
    } else null
}