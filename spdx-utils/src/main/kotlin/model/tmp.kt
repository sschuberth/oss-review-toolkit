package com.here.ort.spdx.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.text.SimpleDateFormat
import java.util.*


// https://gist.github.com/nickrussler/7527851
fun toISO8601UTC(date: Date): String {
    val timeZone = TimeZone.getTimeZone("UTC")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")

    dateFormat.setTimeZone(timeZone)

    return dateFormat.format(date)
}

fun main () {
    println("==== DEBUG ====")

    val mapperConfig: ObjectMapper.() -> Unit = {
        registerKotlinModule()

        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    val spdxElem = SpdxAnnotationType.REVIEW
    val jsonMapper = ObjectMapper().apply(mapperConfig)
    val xmlMapper = XmlMapper().apply(mapperConfig)

    // xmlMapper.enable(SerializationFeature.WRITE_ENUMS_USING_INDEX);

    val xmlSerializedString = xmlMapper.writeValueAsString(spdxElem);
    println("XML Serialized String $xmlSerializedString")

    val jsonSerializedString = jsonMapper.writeValueAsString(spdxElem);
    println("JSON Serialized String $jsonSerializedString")

    val spdxAnnotation = SpdxAnnotation(
        annotator = "Person: Thomas Steenbergen (thomas.steenbergen@here.com)",
        date = Date(),
        type = SpdxAnnotationType.REVIEW,
        comment = "testing 123"
    )

    val spdxAnnotationJsonString = xmlMapper.writeValueAsString(spdxAnnotation);
    println("spdxAnnotationJsonString $spdxAnnotationJsonString")

    val spdxAnnotationXmlString = jsonMapper.writeValueAsString(spdxAnnotation);
    println("JSON Serialized String $spdxAnnotationXmlString")

}
