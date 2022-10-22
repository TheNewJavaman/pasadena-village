package net.javaman.pasadenavillage

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.time.Instant
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val mapper = jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

suspend fun main() {
    val blogs = scrapeDirectory()
    val file = File("Pasadena Village Blogs - ${Instant.now().epochSecond}.json")
    mapper.writeValue(file, blogs)
    logger.info { "Output written to ${file.absolutePath}" }
}
