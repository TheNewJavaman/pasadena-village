package net.javaman.pasadenavillage

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException

private const val DIRECTORY_URL = "https://pasadenavillage.org/content.aspx?page_id=2506&club_id=505865"
private const val MONTH_BASE_URL = "https://pasadenavillage.org/handlers/posts_handler.ashx"
private const val LOG_PROGRESS_INTERVAL = 25

private val RE_BLOG_ID = Regex("""item_id=([0-9]+)(&|$)""")
private val RE_POST_ID = Regex("""getPost\(([0-9]+)\)""")
private val POST_DATE_FORMAT = DateTimeFormatter.ofPattern("[MM][M]/[dd][d]/yyyy, [hh][h]:[mm][m] a")

private val logger = KotlinLogging.logger {}
private val numPostsDiscovered = AtomicInteger()
private val numPostsScraped = AtomicInteger()

suspend fun scrapeDirectory(): List<BlogModel> = coroutineScope {
    logger.info { "Scraping directory ($DIRECTORY_URL)" }
    DriverManager.throttled(DIRECTORY_URL) { driver ->
        driver.findElements(By.cssSelector(".card-contents")).map { el ->
            val title = el.findElement(By.cssSelector(".card-title > a"))
            val blog = BlogModel(
                url = title.getAttribute("href"),
                title = title.text,
                description = el.findElement(By.cssSelector(".card-summary")).text,
                posts = emptyList()
            )
            Pair(blog, async { scrapeBlog(blog.url) })
        }
    }.map { (blog, posts) ->
        blog.copy(posts = posts.await())
    }
}

suspend fun scrapeBlog(url: String): List<PostModel> = coroutineScope {
    logger.info { "Discovered blog ($url)" }
    val blogId = RE_BLOG_ID.find(url)!!.groupValues[1]
    DriverManager.throttled(url) { driver ->
        driver.findElements(By.cssSelector(".date-tree")).map { el ->
            val monthId = el.getAttribute("id")
            val rng = Random.nextInt(0, 10_000) // ClubExpress uses an RNG system... but why?
            val monthUrl = "$MONTH_BASE_URL?blog_id=$blogId&ym=$monthId&_=$rng"
            async { scrapeMonth(monthUrl, url) }
        }
    }.awaitAll().flatten()
}

suspend fun scrapeMonth(url: String, blogUrl: String): List<PostModel> = coroutineScope {
    logger.info { "Discovered month ($url)" }
    DriverManager.throttled(url) { driver ->
        driver.findElements(By.cssSelector("a")).map { el ->
            val onClick = el.getAttribute("onclick")
            val postId = RE_POST_ID.find(onClick)!!.groupValues[1]
            val postUrl = "$blogUrl&pst=$postId"
            async { scrapePost(postUrl) }
        }
    }.awaitAll()
}

suspend fun scrapePost(url: String): PostModel {
    numPostsDiscovered.incrementAndGet()
    logger.info { "Discovered post ($url)" }
    return DriverManager.throttled(url) { driver ->
        PostModel(
            url = url,
            title = driver.findElement(By.cssSelector(".title")).text,
            author = try {
                driver.findElement(By.cssSelector("#title_holder > span"))
            } catch (_: NoSuchElementException) {
                driver.findElement(By.cssSelector("#title_holder > a"))
            }.text,
            postedAt = POST_DATE_FORMAT.parse(
                driver.findElement(By.cssSelector(".post-time > span")).text,
                LocalDateTime::from
            ).atZone(ZoneOffset.systemDefault()).toEpochSecond(),
            contentHtml = driver.findElement(By.cssSelector("#blog_post")).getAttribute("innerHTML")
        )
    }.also {
        val scraped = numPostsScraped.incrementAndGet()
        if (scraped % LOG_PROGRESS_INTERVAL == 0) {
            val discovered = numPostsDiscovered.get()
            logger.info { "Progress: ($scraped scraped / $discovered discovered)" }
        }
    }
}
