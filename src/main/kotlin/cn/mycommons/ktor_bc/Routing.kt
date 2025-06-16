package cn.mycommons.ktor_bc

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import org.slf4j.event.Level

//val logger = LoggerFactory.getLogger("Application")
val logger = KotlinLogging.logger {}

fun Application.configureRouting() {
    install(CallLogging) {
        level = Level.INFO  // 支持 TRACE, DEBUG, INFO, WARN, ERROR
        // filter { call -> call.request.path() != "/health" } // 可排除某些路径
        format { call ->
            "${call.request.httpMethod.value} ${call.request.uri}"
        }
    }
    intercept(ApplicationCallPipeline.Call) {
        val method = call.request.httpMethod
        val path = call.request.uri
        logger.info { "${method.value} $path >>" }
        call.response.pipeline.intercept(ApplicationSendPipeline.After) {
            val method = call.request.httpMethod
            val path = call.request.uri
            val code = call.response.status()?.value
            logger.info { "${method.value} $path <<< code = $code" }
        }

        when (method) {
            HttpMethod.Get -> {
                handleGet(path)
                finish()
            }

            HttpMethod.Put -> {
                handlePut(path)
                finish()
            }

            HttpMethod.Delete -> {
                handleDelete(path)
                finish()
            }

            HttpMethod.Head -> {
                handleHead(path)
                finish()
            }

            else -> {}
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/ok") {
            call.respondText("ok!")
        }
    }
}

private suspend fun PipelineContext<Unit, PipelineCall>.handleDelete(path: String) {
    val cf = CacheFile(path)
    if (!cf.check()) {
        call.respond(HttpStatusCode.Forbidden)
        return
    }
    if (cf.exists()) {
        cf.delete()
        call.respond(HttpStatusCode.OK)
        return
    }
    call.respond(HttpStatusCode.OK)
    return
}

private suspend fun PipelineContext<Unit, PipelineCall>.handleGet(path: String) {
    val cf = CacheFile(path)
    if (!cf.check()) {
        call.respond(HttpStatusCode.Forbidden, "Forbidden access")
        return
    }
    if (cf.exists()) {
        call.respondFile(cf.file)
        return
    }
    call.respond(HttpStatusCode.NotFound, "Not found")
}

private suspend fun PipelineContext<Unit, PipelineCall>.handleHead(path: String) {
    val cf = CacheFile(path)
    if (!cf.check()) {
        call.respond(HttpStatusCode.NotFound, "Forbidden access")
        return
    }
    if (cf.exists()) {
        call.respond(HttpStatusCode.OK)
        return
    }
    call.respond(HttpStatusCode.NotFound, "Not found")
}


private suspend fun PipelineContext<Unit, PipelineCall>.handlePut(path: String) {
    val cf = CacheFile(path)
    if (!cf.check()) {
        call.respond(HttpStatusCode.Forbidden, "Forbidden access")
        return
    }
    runCatching {
        cf.delete()

        val bytes = call.receiveStream().readBytes()
        cf.save(bytes)
        call.respond(HttpStatusCode.Created, "put successfully")
    }.onFailure {
        call.respond(HttpStatusCode.InternalServerError, "put failed")
    }
}
