package com.example.kotlincoroutinesample.ComposingSuspendingFunctions

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.kotlincoroutinesample.R
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class ComposingSuspendingFunctionActivity : AppCompatActivity() {

    var a: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_composing_function)

        a = 5
        when (a) {
            //Sequential By Default:
            //Code in the coroutine, just like in the regular code, is sequential by default.
            //The following example demonstrates it by measuring the total time it takes to execute both suspending functions
            1 -> coroutineFun1()


            //Concurrent Using Async
            //async is just like launch. It starts a separate coroutine which is a light-weight thread that
            // works concurrently with all the other coroutines.
            // The difference is that launch returns a Job and does not carry any resulting value, while async returns
            // a Deferred – a light-weight non-blocking future that represents a promise to provide a result later.
            // You can use .await() on a deferred value to get its eventual result, but Deferred is also a Job, so you can cancel it if needed.
            2 -> coroutineFun2()


            //Lazily Started Async
            //It starts coroutine only when its result is needed by some await or if a start function is invoked
            3 -> coroutineFun3()


            //Async-style Functions
            4->coroutineFun4()


            //Structured Concurrency With Async
            // extract a function that concurrently performs doSomethingUsefulOne and doSomethingUsefulTwo
            // and returns the sum of their results. Because async coroutines builder is defined as extension on
            // CoroutineScope we need to have it in the scope and that is what coroutineScope function provides
            5->coroutineFun5()
        }
    }


    private fun coroutineFun1() = runBlocking {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            println("The answer is ${one + two}")
        }
        println("Completed in $time ms")
    }

    private suspend fun doSomethingUsefulOne(): Int {
        delay(1000L)
        return 12
    }

    private suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L)
        return 10
    }


    //This is twice as fast, because we have concurrent execution of two coroutines.
    //Concurrency with coroutines is always explicit.
    private fun coroutineFun2() = runBlocking {
        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }


    private fun coroutineFun3() = runBlocking {
        val time = measureTimeMillis {
            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
            //some computation
            one.start()
            two.start()
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }


    private fun coroutineFun4() {
        val time = measureTimeMillis {
            // we can initiate async actions outside of a coroutine
            val one = somethingUsefulOneAsync()
            val two = somethingUsefulTwoAsync()
            // but waiting for a result must involve either suspending or blocking.
            // here we use `runBlocking { ... }` to block the main thread while waiting for the result
            runBlocking {
                println("The answer is ${one.await() + two.await()}")
            }
        }
        println("Completed in $time ms")
    }
    // can be used from anywhere. However, their use always implies asynchronous (here meaning concurrent) execution of their
    // action with the invoking code.
    private fun somethingUsefulOneAsync() = GlobalScope.async {
        doSomethingUsefulOne()
    }

    private fun somethingUsefulTwoAsync() = GlobalScope.async {
        doSomethingUsefulTwo()
    }




    private fun coroutineFun5()= runBlocking<Unit> {
        val time = measureTimeMillis {
            println("The answer is ${concurrentSum()}")
        }
        println("Completed in $time ms")
    }
    private suspend fun concurrentSum(): Int = coroutineScope {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        one.await() + two.await()
    }

}