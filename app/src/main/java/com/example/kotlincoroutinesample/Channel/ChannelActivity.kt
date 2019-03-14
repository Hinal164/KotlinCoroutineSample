package com.example.kotlincoroutinesample.Channel

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.kotlincoroutinesample.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce

class ChannelActivity : AppCompatActivity() {

    private var a: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        a = 7
        when (a) {
            //A Channel is conceptually very similar to BlockingQueue.
            // One key difference is that instead of a blocking put operation it has a suspending send,
            // and instead of a blocking take operation it has a suspending receive.
            1 -> channelFun1()


            //Closing and iteration over channels
            //A channel can be closed to indicate that no more elements are coming.
            // On the receiver side it is convenient to use a regular for loop to receive elements from the channel.
            2 -> channelFun2()


            //Building Channel Producers
            //There is a convenient coroutine builder named produce that makes it easy to do it right on producer side,
            // and an extension function consumeEach, that replaces a for loop on the consumer side
            3 -> channelFun3()


            //Pipelines
            //A pipeline is a pattern where one coroutine is producing, possibly infinite, stream of values
            //And another coroutine or coroutines are consuming that stream, doing some processing,
            //and producing some other results
            4 -> channelFun4()


            //Prime Numbers With Pipeline
            5 -> channelFun5()


            //Fan-Out
            //Multiple coroutines may receive from the same channel, distributing work between themselves.
            // Let us start with a producer coroutine that is periodically producing integers
            6 -> channelFun6()


            //Buffered Channels
            //Buffer allows senders to send multiple elements before suspending, similar to the BlockingQueue with a specified
            // capacity, which blocks when buffer is full.
            7->channelFun7()
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




    private fun channelFun2() = runBlocking {
        val channel = Channel<Int>()
        launch {
            for (x in 1..5)
                channel.send(x * x)
            channel.close() // we're done sending
        }
        // here we print received values using `for` loop (until the channel is closed)
        for (y in channel)
            println(y)
        println("Done!")
    }


    private fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
        for (x in 1..5) send(x * x)
    }
    private fun channelFun3() = runBlocking {
        val squares = produceSquares()
        squares.consumeEach { println(it) }
        println("Done!")
    }




    private fun CoroutineScope.produceNumbers() = produce<Int> {
        var x = 1
        while (true)
            send(x++) // infinite stream of integers starting from 1
    }

    private fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
        //consuming the infinite stream and producing the results
        for (x in numbers)
            send(x * x)
    }

    private fun channelFun4() = runBlocking {
        val numbers = produceNumbers() //produce integers from 1 and on
        val squares = square(numbers) //squares integers
        for (i in 1..5) println(squares.receive()) // print first five
        println("Done!")
        coroutineContext.cancelChildren() //channel children coroutines
    }






    private fun CoroutineScope.numbersForm(start: Int) = produce<Int> {
        var x = start
        while (true)
            send(x++)
    }

    private fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
        for (x in numbers)
            if (x % prime != 0)
                send(x)
    }

    private fun channelFun5() = runBlocking {
        var cur = numbersForm(2)
        for (i in 1..10) {
            val prime = cur.receive()
            println(prime)
            cur = filter(cur, prime)

        }
        coroutineContext.cancelChildren()  // cancel all children to let main finish
    }







    private fun CoroutineScope.produceNumber() = produce<Int> {
        var x=1
        while(true){
            send(x++)
            delay(100L)
        }
    }
    private fun CoroutineScope.launchProcessor(id:Int, channel:ReceiveChannel<Int>)=launch {
        for(msg in channel){
            println("Processor $id received $msg")
        }
    }
    private fun channelFun6() = runBlocking {
        val producer=produceNumber()
        repeat(5){
            launchProcessor(it,producer)
        }
        delay(1000)
        producer.cancel() // cancel producer coroutine and thus kill them all

    }





    private fun channelFun7()=runBlocking<Unit>{
        val channel = Channel<Int>(5) // create buffered channel
        val sender = launch { // launch sender coroutine
            repeat(10) {
                println("Sending $it") // print before sending each element
                channel.send(it) // will suspend when buffer is full
            }
        }
        // don't receive anything... just wait....
        delay(1000)
        sender.cancel() // cancel sender coroutine

    }

}