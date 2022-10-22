package net.javaman.pasadenavillage

import io.github.resilience4j.kotlin.ratelimiter.RateLimiterConfig
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.time.Duration
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private val client = HttpClient(CIO)
private val limiter = RateLimiter.of("client", RateLimiterConfig {
    limitForPeriod(1)
    limitRefreshPeriod(Duration.ofMillis(100))
    timeoutDuration(Duration.ofDays(1))
})

suspend fun getPage(url: String): Document = limiter.executeSuspendFunction {
    val res = client.get(url)
    Jsoup.parse(res.bodyAsText(), url)
}
