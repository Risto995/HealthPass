package com.example.healthpassport

import org.jsoup.Jsoup

class UrlParser: Thread() {
    public override fun run() {
        val doc = Jsoup.connect("https://en.wikipedia.org").get()
        val html = doc.outerHtml()
    }
}