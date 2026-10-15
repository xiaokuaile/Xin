package com.xiaokuaile.xin

import android.app.Application
import com.xiaokuaile.xin.data.ClipRepository
import com.xiaokuaile.xin.data.XinDatabase

class XinApp : Application() {
    val repository: ClipRepository by lazy {
        ClipRepository(XinDatabase.get(this).clipDao())
    }
}
