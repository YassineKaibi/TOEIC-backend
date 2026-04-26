package com.toeic.backend.common

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val message: String, val errorCode: String)

@Serializable
data class ListResponse<T>(val items: List<T>)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

suspend inline fun <reified T> RoutingCall.respondList(items: List<T>) {
    if (items.isEmpty()) {
        respond(HttpStatusCode.NoContent)
    } else {
        respond(HttpStatusCode.OK, ListResponse(items))
    }
}

suspend inline fun <reified T> RoutingCall.respondPaged(
    items: List<T>,
    total: Int,
    page: Int,
    pageSize: Int
) {
    respond(HttpStatusCode.OK, PagedResponse(items, total, page, pageSize))
}
