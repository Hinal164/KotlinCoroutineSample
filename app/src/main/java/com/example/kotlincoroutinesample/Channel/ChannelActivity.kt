package com.example.kotlincoroutinesample.Channel

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.kotlincoroutinesample.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ChannelActivity : AppCompatActivity() {

    private var a: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        a = 2
        when (a) {
            //A Channel is conceptually very similar to BlockingQueue.
            // One key difference is that instead of a blocking put operation it has a suspending send,
            // and instead of a blocking take operation it has a suspending receive.
            1 -> channelFun1()


            //Closing and iteration over channels
            //A channel can be closed to indicate that no more elements are coming.
            // On the receiver side it is convenient to use a regular for loop to receive elements from the channel.
            2->channelFun2()
        }
    }

    private fun channelFun1() = runBlocking {
        val channel = Channel<Int>()
        launch {
            for (x in 1..5)
                channel.send(x * x)
        }
        // here we print five received integers:
        repeat(5) {
            println(channel.receive())
        }
        println("Done")
    }


    private fun channelFun2()= runBlocking{
        val channel= Channel<Int>()
        launch{
            for (x in 1..5) channel.send(x * x)
            channel.close() // we're done sending
        }
        // here we print received values using `for` loop (until the channel is closed)
        for (y in channel) println(y)
        println("Done!")
    }
}