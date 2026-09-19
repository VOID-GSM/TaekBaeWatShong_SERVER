package com.example.taekbaewatshongserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableAsync
@EnableScheduling
class TaekBaeWatShongServerApplication

fun main(args: Array<String>) {
	runApplication<TaekBaeWatShongServerApplication>(*args)
}
