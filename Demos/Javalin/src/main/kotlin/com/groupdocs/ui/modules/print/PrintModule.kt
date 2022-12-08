package com.groupdocs.ui.modules.print

import com.groupdocs.ui.model.PrintRequest
import io.javalin.Javalin
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

fun Javalin.printModule() {
    val controller: PrintController by inject(PrintController::class.java)

    post("/viewer/loadDocumentPrint") { ctx ->
        val request = ctx.bodyAsClass(PrintRequest::class.java)
        val decodedGuid = java.net.URLDecoder.decode(request.guid, StandardCharsets.UTF_8.name())
        val guidAsPath = Paths.get(decodedGuid)
        ctx.header("Content-disposition", "attachment; filename=${guidAsPath.fileName}")
        ctx.header("Content-Description", "File Transfer")
        ctx.header("Content-Transfer-Encoding", "binary")
        ctx.header("Cache-Control", "must-revalidate")
        ctx.header("Pragma", "public")
        runBlocking {
            val inputStream = controller.print(request)
            ctx.result(inputStream)
        }
    }
}