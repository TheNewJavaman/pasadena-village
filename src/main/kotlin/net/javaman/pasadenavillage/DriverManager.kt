package net.javaman.pasadenavillage

import io.github.resilience4j.kotlin.ratelimiter.RateLimiterConfig
import io.github.resilience4j.kotlin.ratelimiter.executeFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

private val logger = KotlinLogging.logger {}

object DriverManager {
    private const val NUM_CONCURRENT_DRIVERS = 4
    private const val DRIVER_FETCH_DELAY_MILLIS = 125L

    private val driverPool = ConcurrentLinkedQueue<WebDriver>()
    private val semaphore = Semaphore(NUM_CONCURRENT_DRIVERS)
    private val limiter = RateLimiter.of("driverManager", RateLimiterConfig {
        limitForPeriod(1)
        limitRefreshPeriod(Duration.ofMillis(DRIVER_FETCH_DELAY_MILLIS))
        timeoutDuration(Duration.ofDays(1)) // For our purposes, this is infinite
    })

    init {
        repeat(NUM_CONCURRENT_DRIVERS) {
            driverPool.add(ChromeDriver())
        }
    }

    suspend fun <T> throttled(url: String, block: (WebDriver) -> T): T {
        return semaphore.withPermit {
            val driver = try {
                driverPool.remove()
            } catch (e: NoSuchElementException) {
                logger.warn(e) {
                    "Expected but couldn't find available web driver; " +
                            "this usually happens when shutting down prematurely"
                }
                throw (e)
            }
            limiter.executeFunction {
                driver.get(url)
                block(driver)
            }.also {
                driverPool.add(driver)
            }
        }
    }

    fun release() {
        while (driverPool.isNotEmpty()) {
            driverPool.remove().quit()
        }
    }
}
