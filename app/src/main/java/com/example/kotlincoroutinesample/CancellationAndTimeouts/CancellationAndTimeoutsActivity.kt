package com.example.kotlincoroutinesample.CancellationAndTimeouts

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.kotlincoroutinesample.R
import kotlinx.coroutines.*

class CancellationAndTimeoutsActivity : AppCompatActivity() {

    private var a:Int=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancellation)

        a=6
        when(a){
            //Cancelling Coroutine Execution
            //It gives the control on background coroutines.
            //eg. If user closed the page that launched a coroutine and the result
            //is no longer needed then its operation can be cancelled.
            //The launch function return job that can be used to cancel running coroutine.
            1-> coroutineFun1()


            //Cancellation is cooperative
            //if a coroutine is working in a computation and does not check for cancellation, then it cannot be cancelled.
            2->coroutineFun2()



            //Making computation code cancellable
            //This is possible through:
            //1.Periodically invoke a suspending function that checks for cancellation(yield function is good choice for this)
            //2.The other one is to explicitly check the cancellation status
            3->coroutineFun3()



            //Closing Resources With Finally
            4->coroutineFun4()


            //Run Non-Cancellable Block
            5->coroutineFun5()


            //Timeout
            // you can manually track the reference to the corresponding Job and launch a separate coroutine to cancel
            // the tracked one after delay, there is a ready to use withTimeout function that does it
            6->coroutineFun6()

        }

    }
    private fun coroutineFun1()= runBlocking{
        val job=launch{
            repeat(100){
                i->
                println("Hinal $i")
                delay(500L)
            }
        }
        delay(2000L)
        println("main: I'm tired of waiting!")
        // we don't see any output from the other coroutine because it was cancelled
        job.cancel()
        job.join()
        println("main: Now I can quit.")
    }


    //It continues to print "I'm sleeping" even after cancellation until the job completes by itself after five iterations.
    private fun coroutineFun2()= runBlocking{
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (i < 5) { // computation loop, just wastes CPU
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }
        delay(1000L)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }


    //it cancle the computation function
    private fun coroutineFun3()= runBlocking{
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (isActive) { // cancellable computation loop
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }
        delay(1000L)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }


    private fun coroutineFun4()= runBlocking{
        val job=launch{
            try {
                repeat(10) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                println("I'm running finally")
            }
        }
        delay(1000L)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }

    private fun coroutineFun5()= runBlocking{
        val job=launch{
            try {
                repeat(10) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                withContext(NonCancellable) {
                    println("I'm running finally")
                    delay(1000L)
                    println("And I've just delayed for 1 sec because I'm non-cancellable")
                }
            }
        }
        delay(1000L)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }


    private fun coroutineFun6()= runBlocking {
        //use withTimeoutOrNull function that is similar to withTimeout,
        // but returns null on timeout instead of throwing an exception:
        withTimeout(1300L) {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
    }
}