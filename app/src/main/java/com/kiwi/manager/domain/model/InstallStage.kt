package com.kiwi.manager.domain.model

enum class InstallStage {
    IDLE,
    FETCHING_INFO,
    DOWNLOADING,
    CONNECTING_ADB,
    WAKING_WATCH,
    PUSHING_TO_WATCH,
    INSTALLING_ON_WATCH,
    OPENING_INSTALLER,
    SUCCESS,
    ERROR
}
