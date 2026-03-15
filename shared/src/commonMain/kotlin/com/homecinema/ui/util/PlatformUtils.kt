package com.homecinema.ui.util

enum class DeviceType { MOBILE, TABLET, DESKTOP }

expect fun getDeviceType(): DeviceType
