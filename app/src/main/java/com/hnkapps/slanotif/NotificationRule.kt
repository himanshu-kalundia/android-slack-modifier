package com.hnkapps.slanotif

data class NotificationRule(
    val id: String,
    var channelName: String,
    var soundUri: String?,
    var soundName: String? = null
)
