package com.example.kotlincoroutinesample.SelectExpression

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.kotlincoroutinesample.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class SelectExpressionActivity : AppCompatActivity() {

    /**
     * Select expression makes it possible to await multiple suspending functions simultaneously and
     * select the first one that becomes available.
     */

    private var a: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_expression)

        a = 6
        when (a) {

            //Selecting From Channels
            1 -> expressionFun1()


            //Selecting On Close
            2 -> expressionFun2()


            //Selecting To Send
            3 -> expressionFun3()

            //Selecting Deferred Values
            4->expressionFun4()

            //Switch over a channel of deferred values
            5->expressionFun5()


            //Thread confinement fine-grained
            6->function1()
        }
    }


    private fun expressionFun1() = runBlocking {
        val fizz = fizz()
        val buzz = buzz()
        repeat(7) {
            selectFizzBuzz(fizz, buzz)
        }
        coroutineContext.cancelChildren() // cancel fizz & buzz coroutines
    }

    private fun CoroutineScope.fizz() = produce<String> {
        while (true) { // sends "Fizz" every 300 ms
            delay(300)
            send("Fizz")
        }
    }

    private fun CoroutineScope.buzz() = produce<String> {
        while (true) { // sends "Buzz!" every 500 ms
            delay(500)
            send("Buzz!")
        }
    }

    //Using receive suspending function we can receive either from one channel or the other.
    // But select expression allows us to receive from both simultaneously using its onReceive clauses
    private suspend fun selectFizzBuzz(fizz: ReceiveChannel<String>, buzz: ReceiveChannel<String>) {
        select<Unit> {
            // <Unit> means that this select expression does not produce any result
            fizz.onReceive { value ->
                // this is the first select clause
                println("fizz -> '$value'")
            }
            buzz.onReceive { value ->
                // this is the second select clause
                println("buzz -> '$value'")
            }
        }
    }




    //The onReceive clause in select fails when the channel is closed causing the corresponding select to throw an exception.
    // We can use onReceiveOrNull clause to perform a specific action when the channel is closed.
    private suspend fun selectAorB(a: ReceiveChannel<String>, b: ReceiveChannel<String>): String =
        select<String> {
            a.onReceiveOrNull { value ->
                if (value == null)
                    "Channel 'a' is closed"
                else
                    "a->$value"
            }
            b.onReceiveOrNull { value ->
                if (value == null)
                    "Channel 'b' is closed"
                else
                    "b->$value"
            }
        }
    private fun expressionFun2() = runBlocking {
        val a = produce<String> {
            repeat(4) { send("Hello $it") }
        }
        val b = produce<String> {
            repeat(4) { send("World $it") }
        }
        repeat(8) {
            // print first eight results
            println(selectAorB(a, b))
        }
        coroutineContext.cancelChildren()
    }





    private fun expressionFun3()= runBlocking {
        val side = Channel<Int>() // allocate side channel
        launch { // this is a very fast consumer for the side channel
            side.consumeEach { println("Side channel has $it") }
        }
        produceNumbers(side).consumeEach {
            println("Consuming $it")
            delay(250) // let us digest the consumed number properly, do not hurry
        }
        println("Done consuming")
        coroutineContext.cancelChildren()
    }
    private fun CoroutineScope.produceNumbers(side: SendChannel<Int>) = produce<Int> {
        for (num in 1..10) { // produce 10 numbers from 1 to 10
            delay(100) // every 100 ms
            select<Unit> {
                onSend(num) {} // Send to the primary channel
                side.onSend(num) {} // or to the side channel
            }
        }
    }





    private fun expressionFun4()= runBlocking {
        val list = asyncStringsList()
        val result = select<String> {
            list.withIndex().forEach { (index, deferred) ->
                deferred.onAwait { answer ->
                    "Deferred $index produced answer '$answer'"
                }
            }
        }
        println(result)
        val countActive = list.count { it.isActive }
        println("$countActive coroutines are still active")
    }
    private fun CoroutineScope.asyncString(time: Int) = async {
        delay(time.toLong())
        "Waited for $time ms"
    }

    private fun CoroutineScope.asyncStringsList(): List<Deferred<String>> {
        val random = Random(3)
        return List(12) { asyncString(random.nextInt(1000)) }
    }




    private fun expressionFun5()= runBlocking {
        val chan = Channel<Deferred<String>>() // the channel for test
        launch { // launch printing coroutine
            for (s in switchMapDeferred(chan))
                println(s) // print each received string
        }
        chan.send(asyncString("BEGIN", 100))
        delay(200) // enough time for "BEGIN" to be produced
        chan.send(asyncString("Slow", 500))
        delay(100) // not enough time to produce slow
        chan.send(asyncString("Replace", 100))
        delay(500) // give it time before the last one
        chan.send(asyncString("END", 500))
        delay(1000) // give it time to process
        chan.close() // close the channel ...
        delay(500) // and wait some time to let it finish
    }
    private fun CoroutineScope.switchMapDeferred(input: ReceiveChannel<Deferred<String>>) = produce<String> {
        var current = input.receive() // start with first received deferred value
        while (isActive) { // loop while not cancelled/closed
            val next = select<Deferred<String>?> { // return next deferred value from this select or null
                input.onReceiveOrNull { update ->
                    update // replaces next value to wait
                }
                current.onAwait { value ->
                    send(value) // send value that current deferred has produced
                    input.receiveOrNull() // and use the next deferred from the input channel
                }
            }
            if (next == null) {
                println("Channel was closed")
                break // out of loop
            } else {
                current = next
            }
        }
    }

    private fun CoroutineScope.asyncString(str: String, time: Long) = async {
        delay(time)
        str
    }


    private suspend fun CoroutineScope.massiveRun(action: suspend () -> Unit) {
        val n = 100  // number of coroutines to launch
        val k = 1000 // times an action is repeated by each coroutine
        val time = measureTimeMillis {
            val jobs = List(n) {
                launch {
                    repeat(k) { action() }
                }
            }
            jobs.forEach { it.join() }
        }
        println("Completed ${n * k} actions in $time ms")
    }

    private val counterContext = newSingleThreadContext("CounterContext")
    private var counter = 0
    private fun function1()= runBlocking {
        /*GlobalScope.massiveRun { // run each coroutine with DefaultDispathcer
            withContext(counterContext) { // but confine each increment to the single-threaded context
                counter++
            }
        }*/
        CoroutineScope(counterContext).massiveRun { // run each coroutine in the single-threaded context
            counter++
        }
        println("Counter = $counter")
    }
}