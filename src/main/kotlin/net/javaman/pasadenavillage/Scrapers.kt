package net.javaman.pasadenavillage

import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.random.nextInt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import org.openqa.selenium.chrome.ChromeDriver

private val RE_BLOG_ID = Regex("""item_id=([0-9]+)(&|$)""")
private val RE_POST_ID = Regex("""getPost\(([0-9]+)\)""")

private val logger = KotlinLogging.logger {}
private val driver = ChromeDriver()
private val numPostsDiscovered = AtomicInteger(0)
private val numPostsScraped = AtomicInteger(0)

suspend fun scrapeDirectory() = coroutineScope {
    getPage("https://pasadenavillage.org/content.aspx?page_id=2506&club_id=505865")
        .select(".card-contents")
        .map {
            async {
                val title = it.selectFirst(".card-title > a")!!
                val summary = it.selectFirst(".card-summary")!!
                val blogUrl = "https://pasadenavillage.org" + title.attr("href")
                logger.info { "Discovered blog $blogUrl" }
                BlogModel(
                    url = blogUrl,
                    title = title.text(),
                    description = summary.text(),
                    posts = scrapeBlog(blogUrl)
                )
            }
        }
        .awaitAll()
}

suspend fun scrapeBlog(url: String) = coroutineScope {
    val blogId = RE_BLOG_ID.find(url)!!.groupValues[1]
    getPage(url)
        .select(".date-tree")
        .map {
            async {
                val monthId = it.id()
                val rng = Random.nextInt(0..10_000)
                val monthUrl = "https://pasadenavillage.org/handlers/posts_handler.ashx" +
                        "?blog_id=$blogId&ym=$monthId&_=$rng"
                logger.info { "Discovered month $monthUrl" }
                scrapeMonth(monthUrl, url)
            }
        }
        .awaitAll()
        .flatten()
}

suspend fun scrapeMonth(url: String, blogUrl: String) = coroutineScope {
    getPage(url)
        .select("a")
        .map {
            async {
                val onClick = it.attr("onclick")
                val postId = RE_POST_ID.find(onClick)!!.groupValues[1]
                val postUrl = "$blogUrl&pst=$postId"
                logger.info { "Discovered post $postUrl" }
                scrapePost(postUrl)
            }
        }
        .awaitAll()
}

suspend fun scrapePost(url: String): PostModel {
    numPostsDiscovered.incrementAndGet()
    val doc = getPage(url)
    val post = PostModel(
        url = url,
        title = doc.selectFirst(".title")!!.text(),
        author = doc.selectFirst("#title_holder > span")?.text()
            ?: doc.selectFirst("#title_holder > a")!!.text(),
        postedAt = doc.selectFirst(".post-time > span")!!.text().toLong(),
        contentHtml = doc.selectFirst("#blog_post")!!.html()
    )
    val scraped = numPostsScraped.incrementAndGet()
    if (scraped % 25 == 0) {
        val discovered = numPostsDiscovered.get()
        logger.info { "Progress: $scraped scraped / $discovered discovered" }
    }
    return post
}
