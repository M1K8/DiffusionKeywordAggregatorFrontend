package com.pushtechnology.diffusion

import com.pushtechnology.diffusion.client.Diffusion
import com.pushtechnology.diffusion.client.callbacks.ErrorReason
import com.pushtechnology.diffusion.client.features.Topics
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification
import com.pushtechnology.diffusion.datatype.json.JSON
import kotlinx.coroutines.*
import org.knowm.xchart.style.Styler
import org.json.*
import org.knowm.xchart.*
import java.util.*
import org.knowm.xchart.XYSeries


class Main {

    companion object {
        var chart : XYChart? = null
        var sw : SwingWrapper<XYChart?>? = null

        //start half an hour in the past
        val start = Date(System.currentTimeMillis() - 300000)

        var xResultsT : MutableList<Date> = mutableListOf(start)
        var yResultsT : MutableList<Int> = mutableListOf(0)

        var xResultsR : MutableList<Date> = mutableListOf(start)
        var yResultsR : MutableList<Int> = mutableListOf(0)


        class JsonStream : Topics.ValueStream<JSON> {
            override fun onError(p0: ErrorReason?) {
                println("oh dear")
            }

            override fun onClose() {
                println("Closed")
            }

            override fun onSubscription(p0: String?, p1: TopicSpecification?) {
                println("Subbed to $p0")
            }

            override fun onValue(
                    topicPath: String,
                    specification: TopicSpecification,
                    oldValue: JSON?,
                    newValue: JSON) {
                println("New Value of $topicPath is ${newValue.toJsonString()}")


                val e = JSONObject(newValue.toJsonString())

                val count = e.getInt("count")
                val time = (e.getString("timestamp")).toLong() *1000


                when (e.getString("website")) {
                    "reddit" -> {
                        yResultsR.add(count)
                        println(Date(time))
                        xResultsR.add(Date(time))

                        chart?.updateXYSeries("Reddit", xResultsR , yResultsR, null)
                    }
                    "twitter" -> {
                        yResultsT.add(count)
                        println(Date(time))
                        xResultsT.add(Date(time))

                        chart?.updateXYSeries("Twitter", xResultsT , yResultsT, null)
                    }
                }

                sw?.repaintChart()
            }

            override fun onUnsubscription(
                    topicPath: String,
                    specification: TopicSpecification,
                    reason: Topics.UnsubscribeReason) {
            }
        }
        private suspend fun initStuff() {
            val _session = Diffusion.sessions().principal("admin").password("password").open("ws://localhost:8080")

            try {

                val authSetup = GlobalScope.launch {
                    _session.feature(AuthenticationControl::class.java).setAuthenticationHandler("before-system-handler", Auth())
                }


                val missingSetup = GlobalScope.launch {
                    _session.feature(TopicControl::class.java).addMissingTopicHandler("*.*", MissingHandler())
                }

                authSetup.join()
                missingSetup.join()

                _session.close()
            } catch (e: Exception) {
                _session.close()
            }

        }


        private fun showChart() {
            chart = XYChartBuilder().width(400).height(100).title("Keyword Aggregator Demo").xAxisTitle("Time").yAxisTitle("Occurrences").build().also {
                it.addSeries("Twitter",   xResultsT, yResultsT)
                it.addSeries("Reddit",  xResultsR, yResultsR)
            }
            chart?.styler?.apply {
                legendPosition = Styler.LegendPosition.InsideNE
                defaultSeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Area
                yAxisDecimalPattern = "#"
            }
            sw = SwingWrapper(chart).also {
                it.displayChart()
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            showChart()
            println("Starting...")
            runBlocking { initStuff() }
            println("Done setting up handlers!")

            val session = Diffusion.sessions().principal("admin").password("password").open("ws://localhost:8080")

            val topJ = Topics::class.java


            session.feature(TopicControl::class.java).removeTopics("yeet")
            session.feature(TopicControl::class.java).removeTopics("count")

            session.feature(topJ).addStream("yeet", JSON::class.java, JsonStream())
            session.feature(topJ).createTopicView("count", "map >yeet to count as <value(/info)>")

            session.feature(topJ).addStream("count", JSON::class.java, JsonStream())

            session.feature(topJ).subscribe("count")
            println("Subbed!")


            Thread.sleep(Long.MAX_VALUE)



        }
    }

}