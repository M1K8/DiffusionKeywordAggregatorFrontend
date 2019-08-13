package com.pushtechnology.diffusion

import com.pushtechnology.diffusion.client.Diffusion
import com.pushtechnology.diffusion.client.callbacks.ErrorReason
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl
import com.pushtechnology.diffusion.client.session.*
import com.pushtechnology.diffusion.client.topics.details.TopicType
import java.util.*

class MissingHandler : TopicControl.MissingTopicNotificationStream {

    inner class PropCallback : ClientControl.SessionPropertiesCallback {
        override fun onReply(p0: SessionId?, p1: MutableMap<String, String>) {
            vals = p1
        }

        override fun onUnknownSession(p0: SessionId?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onError(p0: ErrorReason?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    var vals = mutableMapOf<String,String>()
    override fun onMissingTopic(p0: TopicControl.MissingTopicNotification?) {
        val session = Diffusion.sessions().principal("admin").password("password").open("ws://localhost:8080")
        val topicCtl = session.feature(TopicControl::class.java)

        val strL = listOf(Session.PRINCIPAL)

        session.feature(ClientControl::class.java).getSessionProperties(p0?.sessionId,strL,PropCallback())

        if (vals.isNotEmpty())
        {
            when (vals[Session.PRINCIPAL]) {
                "admin" -> {
                    topicCtl.addTopic(p0?.topicPath, TopicType.JSON)
                    p0?.proceed()
                }
                else -> p0?.cancel()
            }
        }
        session.close()

    }

    override fun onError(p0: ErrorReason?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}