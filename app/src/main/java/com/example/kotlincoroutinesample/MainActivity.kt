package com.example.kotlincoroutinesample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private var a: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        a = 8
        when (a) {
            //non-blocking delay(...) and blocking Thread.sleep(...)
            1 -> simpleCoroutineFun()

            //Bridging blocking and non-blocking.
            2 -> coroutineFun1()
            // It is easy to lose track of which one is blocking and which one is not
            //this code uses only non-blocking delay

            3 -> coroutineFun2()

            4 -> coroutineJob()
            //Delaying for a time while another coroutine is working is not a good approach.
            // Let's explicitly wait (in a non-blocking way) until the background Job that we have launched is complete


            //Structured concurrency
            //There is a better solution. We can use structured concurrency in our code. Instead of launching coroutines in the GlobalScope, just like we usually do with threads (threads are always global),
            //we can launch coroutines in the specific scope of the operation we are performing.
            5 -> coroutineWithoutJob()


            //Scope builder
            6 -> coroutineScopeBuilder()


            //Extract function refactoring
            7 -> coroutineFunctionRefactoring()

            8 -> coroutineFun3()
        }

    }


    private fun simpleCoroutineFun() {
        GlobalScope.launch {
            //lifetime of the new coroutine is limited only by the lifetime of the whole application
            delay(5000L)// non-blocking delay for 5(include 3 sec of main thread + 2 sec = 5) second
            println("world!") // print after delay
        }
        println("Hello") //main thread continues while coroutine is delayed
        Thread.sleep(3000L) // block main thread for 3 seconds to keep JVM alive
        println("Hinal")

        //delay(2000L)
        //Error: Suspend functions are only allowed to be called from a coroutine or another suspend function
        //That is because delay is a special suspending function that does not block a thread
        // but suspends coroutine and it can be only used from a coroutine.
    }

    private fun coroutineFun1() {
        GlobalScope.launch {
            // launch new coroutine in background and continue
            delay(5000L)
            println("world!")
        }
        println("Hello") // main thread continues here immediately
        runBlocking {
            // but this expression blocks the main thread
            delay(3000L) // ... while we delay for 2 seconds to keep JVM alive
        }
        println("Hinal")
    }

    private fun coroutineFun2() = runBlocking<Unit> {
        // start main coroutine
        //Here runBlocking<Unit> { ... } works as an adaptor that is used to start the top-level main coroutine

        GlobalScope.launch {
            // launch new coroutine in background and continue
            delay(5000L)
            println("World!")
        }
        println("Hello,") // main coroutine continues here immediately
        delay(3000L) // delaying for 3 seconds to keep JVM alive
        println("Hinal")
    }

    private fun coroutineJob() = runBlocking {
        val job = GlobalScope.launch {
            // launch new coroutine and keep a reference to its Job
            delay(3000L)
            println("World!")
        }
        println("Hello,")
        job.join() // wait until child coroutine completes
    }


    //We can launch coroutines in this scope without having to join them explicitly,
    // because an outer coroutine (runBlocking in our example) does not complete until
    // all the coroutines launched in its scope complete.
    private fun coroutineWithoutJob() = runBlocking {
        // this: CoroutineScope
        launch {
            // launch new coroutine in the scope of runBlocking
            delay(3000L)
            println("World!")
        }
        println("Hello")
    }

    // It creates new coroutine scope and does not complete until all launched children complete.
    // The main difference between runBlocking and coroutineScope is that the latter does not block the
    // current thread while waiting for all children to complete.
    private fun coroutineScopeBuilder() = runBlocking {
        // this: CoroutineScope
        launch {
            delay(1000L)
            println("Task from runBlocking")
        }

        coroutineScope {
            // Creates a new coroutine scope
            launch {
                delay(1000L)
                println("Task from nested launch")
            }

            delay(1000L)
            println("Task from coroutine scope") // This line will be printed before nested launch
        }

        println("Coroutine scope is over") // This line is not printed until nested launch completes
    }


    private fun coroutineFunctionRefactoring() = runBlocking {
        launch { doWorld() }
        println("Hello,")
    }

    //suspending function: Used inside coroutine function just like regular function
    //which having additional feature that they can use other suspending function like delay
    private suspend fun doWorld() {
        delay(3000L)
        println("World!")
    }

    //Active coroutines that were launched in GlobalScope do not keep the process alive. They are like daemon threads.
    private fun coroutineFun3()= runBlocking {
        GlobalScope.launch {
            repeat(10){
                i->
                println(" Hinal $i")
                delay(1000L)
            }
        }
        delay(2000L)
        println("Hinal Vekariya")
    }
}
