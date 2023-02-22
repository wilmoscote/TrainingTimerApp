package com.wmsoftware.trainingtimer.utils

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            //
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {

        }
    }

    override fun onNewToken(token: String) {
        //
    }
}