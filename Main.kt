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
        val topics = mutableListOf("");

        //start 30 seconds in the past
        private val start = Date(System.currentTimeMillis() - 300000)


        // topic : <reddit, twitter>
        var xResults : MutableMap<String,MutableList<MutableList<Date>>>? = mutableMapOf(Pair("", mutableListOf(mutableListOf(Date(0)))))// = HashMap("reddit"(mutableListOf(start)))

        var yResults : MutableMap<String,MutableList<MutableList<Int>>>? = mutableMapOf(Pair("", mutableListOf(mutableListOf(0))))

        var keyword = ""


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

                if (! topics.contains(topicPath))
                {
                    topics.add(topicPath)
                    println("ye");
                    xResults?.apply {
                        put(topicPath, mutableListOf(mutableListOf(start)))
                        get(topicPath)?.add(mutableListOf(start))
                    }

                    yResults?.apply {
                        put(topicPath, mutableListOf(mutableListOf(0)))
                        get(topicPath)?.add(mutableListOf(0))
                    }

                    chart?.addSeries("$topicPath- Reddit",  xResults!![topicPath]!![0] , yResults!![topicPath]!![0])

                    chart?.addSeries("$topicPath- Twitter", xResults!![topicPath]!![1] , yResults!![topicPath]!![1])

                }



                val e = JSONObject(newValue.toJsonString())

                val count = e.getInt("count")
                val time = (e.getString("timestamp")).toLong() *1000


                when (e.getString("website")) {
                    "reddit" -> {
                        yResults?.get(topicPath)!![0].add(count)
                        println(Date(time))
                        xResults?.get(topicPath)!![0].add(Date(time))

                        chart?.updateXYSeries("$topicPath- Reddit", xResults?.get(topicPath)!![0] , yResults?.get(topicPath)!![0], null)
                    }
                    "twitter" -> {
                        yResults?.get(topicPath)!![1].add(count)
                        println(Date(time))
                        xResults?.get(topicPath)!![1].add(Date(time))

                        chart?.updateXYSeries("$topicPath- Twitter", xResults?.get(topicPath)!![1] , yResults?.get(topicPath)!![1], null)
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
            chart?.styler?.apply {
                legendPosition = Styler.LegendPosition.InsideNE
                defaultSeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Area
                yAxisDecimalPattern = "#.#"
                yAxisTickMarkSpacingHint = 50
            }


            sw = SwingWrapper(chart).also {
                it.displayChart()
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            keyword = if (args.isNotEmpty()) {
                args[0]
            } else "apple"
            println("Starting...")
            runBlocking { initStuff() }
            println("Done setting up handlers!")
            val session = Diffusion.sessions().principal("admin").password("password").open("ws://localhost:8080")

            val topJ = Topics::class.java



            //session.feature(topJ).addStream(keyword, JSON::class.java, JsonStream())
            //session.feature(topJ).createTopicView("count", "map >$keyword to count as <value(/info)>")

            //session.feature(TopicControl::class.java).removeTopics("count")

            session.feature(topJ).addStream("*count//", JSON::class.java, JsonStream())
            session.feature(topJ).subscribe("*count//")

            chart = XYChartBuilder().width(1280).height(720).title("Keyword Aggregator Demo").xAxisTitle("Time (hh:mm:ss)").yAxisTitle("Occurrences (Avg per 10 seconds)").build()

            println("Subbed!")
            showChart()




            Thread.sleep(Long.MAX_VALUE)



        }
    }

}