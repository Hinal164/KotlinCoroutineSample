package com.example.kotlincoroutinesample.CoroutineContextAndDispatchers

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.kotlincoroutinesample.R
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class CoroutineContextAndDispatcherActivity : AppCompatActivity(), CoroutineScope {


    lateinit var job: Job

    private fun create() {
        job = Job()
    }

    private fun destroy() {
        job.cancel()
    }

    private fun doSomething() {
        // launch ten coroutines for a demo, each working for a different time
        repeat(10) { i ->
            launch {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, ... etc
                println("Coroutine $i is done")
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() =  Dispatchers.Default + job


    private var a: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dispatcher)

        a = 8
        when (a) {
            //Dispatchers And Threads
            1 -> dispatcherFun1()


            //Unconfined vs confined dispatcher
            //The Dispatchers.Unconfined coroutine dispatcher starts coroutine in the caller thread, but only until
            // first suspension point. After suspension it resumes in the thread that is fully determined
            // by the suspending function that was invoked.
            2 -> dispatcherFun2()


            //Debugging coroutines and threads
            3 -> dispatcherFun3()


            //Jumping Between Threads
            4 -> dispatcherFun4()


            //Children of a Coroutine
            5->dispatcherFun5()


            //Parental Responsibilities
            6->dispatcherFun6()


            //Naming Coroutines For Debugging
            // when coroutine is tied to the processing of a specific request or doing some specific background task,
            // it is better to name it explicitly for debugging purposes. CoroutineName context element serves the
            // same function as a thread name. It'll get displayed in the thread name that is executing this coroutine
            // when debugging mode is turned on.
            7->dispatcherFun7()


            //Cancellation Via Explicit Job
            8->dispatcherFun8()
        }
    }



    private fun dispatcherFun1() = runBlocking {
        launch {
            // context of the parent, main runBlocking coroutine
            println("main runBlocking      : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            println("Unconfined            : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Default) {
            // will get dispatched to DefaultDispatcher
            //is used when coroutines are launched in GlobalScope, is represented by Dispatchers.Default
            println("Default               : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(newSingleThreadContext("MyOwnThread")) {
            // will get its own new thread
            println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
        }
    }


    private fun dispatcherFun2() = runBlocking {
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
            delay(500)
            println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
        }
        launch {
            // context of the parent, main runBlocking coroutine
            println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
        }
    }


    //There are three coroutines. The main coroutine (#1) – runBlocking one, and two coroutines computing deferred values a (#2)
    // and b (#3). They are all executing in the context of runBlocking and are confined to the main thread.
    private fun dispatcherFun3() = runBlocking {
        val a = async {
            log("I'm computing a piece of the answer")
            6
        }
        val b = async {
            log("I'm computing another piece of the answer")
            7
        }
        log("The answer is ${a.await() * b.await()}")
    }

    private fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")


    private fun dispatcherFun4() {
        //"use" function from the Kotlin standard library to release threads that are created with newSingleThreadContext
        // when they are no longer needed.
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    log("Started in ctx1")
                    withContext(ctx2) {
                        log("Working in ctx2")
                    }
                    log("Back to ctx1")
                }
            }
        }
    }


    //When a coroutine is launched in the CoroutineScope of another coroutine, it inherits its context via
    // CoroutineScope.coroutineContext and the Job of the new coroutine becomes a child of the parent coroutine's job.
    // When the parent coroutine is cancelled, all its children are recursively cancelled, too.
    private fun dispatcherFun5()= runBlocking{

        // launch a coroutine to process some kind of incoming request
        val request = launch {
            // it spawns two other jobs, one with GlobalScope
            GlobalScope.launch {
                println("job1: I run in GlobalScope and execute independently!")
                delay(1000)
                println("job1: I am not affected by cancellation of the request")
            }
            // and the other inherits the parent context
            launch {
                delay(100)
                println("job2: I am a child of the request coroutine")
                delay(1000)
                println("job2: I will not execute this line if my parent request is cancelled")
            }
        }
        delay(500)
        request.cancel() // cancel processing of the request
        delay(1000) // delay a second to see what happens
        println("main: Who has survived request cancellation?")
    }




    //A parent coroutine always waits for completion of all its children. Parent does not have to explicitly track all the
    //children it launches and it does not have to use Job.join to wait for them at the end
    private fun dispatcherFun6()= runBlocking {

        // launch a coroutine to process some kind of incoming request
        val request = launch {
            repeat(3) { i -> // launch a few children jobs
                launch  {
                    delay((i + 1) * 200L) // variable delay 200ms, 400ms, 600ms
                    println("Coroutine $i is done")
                }
            }
            println("request: I'm done and I don't explicitly join my children that are still active")
        }
        request.join() // wait for completion of the request, including all its children
        println("Now processing of the request is complete")
    }


    private fun dispatcherFun7()= runBlocking {

        log("Started main coroutine")
        // run two background value computations
        val v1 = async(CoroutineName("v1coroutine")) {
            delay(500)
            log("Computing v1")
            252
        }
        val v2 = async(CoroutineName("v2coroutine")) {
            delay(1000)
            log("Computing v2")
            6
        }
        log("The answer for v1 / v2 = ${v1.await() / v2.await()}")
    }


    private fun dispatcherFun8()= runBlocking {
        val activity = CoroutineContextAndDispatcherActivity()
        activity.create() // create an activity
        activity.doSomething() // run test function
        println("Launched coroutines")
        delay(500L) // delay for half a second
        println("Destroying activity!")
        activity.destroy() // cancels all coroutines
        delay(1000) // visually confirm that they don't work
    }
}