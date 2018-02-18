package com.poterion.monitor.notifiers.raspiw2812

import com.poterion.monitor.notifiers.raspiw2812.control.SerialPortCommunicator

fun main(args: Array<String>) {
    SerialPortCommunicator.findCommunicator()?.apply {
        //sendMessage(listOf("lighthouse 32,0,0 32,0,0 50 5 20 0 100"))
        //sendMessage(listOf("lighthouse 0,32,0 0,32,0 50 5 20 0 100"))
        //sendMessage(listOf("lighthouse 0,0,32 0,0,32 50 5 20 0 100"))
        //sendMessage(listOf("lighthouse 32,32,0 32,32,0 50 5 20 0 100"))
        //sendMessage(listOf("lighthouse 0,32,32 0,32,32 50 5 20 0 100"))
        //sendMessage(listOf("lighthouse 32,0,32 32,0,32 50 5 20 0 100"))
        //sendMessage(listOf("lighthouse 8,8,8 8,8,8 60 5 20 0 100"))
        //sendMessage(listOf("wipe 0,255,0 0,0,255 50 3 1000 1000 100"))
        //sendMessage(listOf("rotation 255,255,255 255,255,255 50 10 10 0 100"))
        //sendMessage(listOf("chaise 0,255,255 255,255,0 10 10 10 0 100"))
        //sendMessage(listOf("fadeToggle 255,255,0 0,255,255 10 3 0 20 80"))
        //sendMessage(listOf("rotation 0,0,16 0,0,16 50 12 8 0 100"))
        //sendMessage(listOf("light 255,255,255 255,255,255 50 3 0 0 100"))
        //sendMessage(listOf("light 0,0,0 0,0,0 50 3 0 0 100"))
        //sendMessage(listOf("spin 8,8,8 8,8,8 50 3 0 0 100"))

        sendMessage(listOf("fade 16,16,16 16,16,16 20 3 0 80 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("fadeToggle 16,16,16 16,16,16 10 3 0 60 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("fade 0,16,0 0,16,0 30 3 0 80 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("wipe 0,16,16 0,16,16 50 3 2000 0 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("wipe 0,10,16 0,10,16 50 3 1 0 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("rotation 0,0,16 0,0,16 50 12 8 0 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("rotation 16,0,0 16,0,0 50 12 8 0 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("fadeToggle 20,12,0 20,12,0 5 3 0 0 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("lighthouse 16,0,0 16,0,0 50 12 8 0 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("chaise 16,0,0 16,0,0 10 12 8 0 100"))
        Thread.sleep(5_000L)
        sendMessage(listOf("light 0,0,0 0,0,0 50 3 0 0 100"))
    }
}