package com.example.myapp2.utils

class VLogUtils {
    companion object {
        fun writeLogWithPrefix(myObj: Any? = null, myFuncName: String?, logContent: String?) {
                val myClassName = myObj?.let { it.javaClass.kotlin.simpleName }
                //     val myFuncName = this.javaClass.enclosingMethod?.name
                val prefix = "Class:$myClassName Func:$myFuncName"
                println("####  $prefix $logContent ####")
        }
    }
}