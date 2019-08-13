package com.pushtechnology.diffusion

import com.pushtechnology.diffusion.client.callbacks.ErrorReason
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl
import com.pushtechnology.diffusion.client.security.authentication.Authenticator
import com.pushtechnology.diffusion.client.types.Credentials

class Auth : AuthenticationControl.ControlAuthenticator {
    override fun authenticate(p0: String?, p1: Credentials?, p2: MutableMap<String, String>?, p3: MutableMap<String, String>?, p4: Authenticator.Callback) {
        when (p0) {
            "admin" -> p4.allow(p3)
            "client" -> p4.allow()
            "block" -> p4.deny()

            else -> p4.abstain()
        }
    }

    override fun onError(p0: ErrorReason?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}