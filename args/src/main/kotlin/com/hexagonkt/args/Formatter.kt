package com.hexagonkt.args

interface Formatter<T> {
    fun summary(component: T): String
    fun definition(component: T): String
    fun detail(component: T): String
}