package com.groupdocs.ui.viewer.micronaut.modules.rotate

import com.groupdocs.ui.viewer.micronaut.cache.FilesCache
import com.groupdocs.ui.viewer.micronaut.cache.MemoryFilesCacheEntry
import com.groupdocs.ui.viewer.micronaut.cache.MemoryFilesCachePage
import com.groupdocs.ui.viewer.micronaut.config.ApplicationConfig
import com.groupdocs.ui.viewer.micronaut.manager.PathManager
import com.groupdocs.ui.viewer.micronaut.model.RotateRequest
import com.groupdocs.ui.viewer.micronaut.model.RotateResponse
import com.groupdocs.ui.viewer.micronaut.usecase.AreFilesSupportedUseCase
import com.groupdocs.ui.viewer.micronaut.usecase.RetrieveLocalFilePagesDataUseCase
import com.groupdocs.ui.viewer.micronaut.util.InternalServerException
import com.groupdocs.ui.viewer.micronaut.util.angleToRotation
import com.groupdocs.ui.viewer.micronaut.util.rotationToAngle
import com.groupdocs.viewer.options.Rotation
import io.micronaut.context.annotation.Bean
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.net.URLDecoder

interface RotateBean {
    suspend fun rotate(request: RotateRequest): RotateResponse
}

@Bean(typed = [RotateBean::class])
@Singleton
class RotateBeanImpl(
    @Inject private val areFilesSupported: AreFilesSupportedUseCase,
    @Inject private val retrieveLocalFilePagesStream: RetrieveLocalFilePagesDataUseCase,
    @Inject private val pathManager: PathManager,
    @Inject private val appConfig: ApplicationConfig,
    @Inject private val filesCache: FilesCache
) : RotateBean {
    override suspend fun rotate(request: RotateRequest): RotateResponse {
        val password = request.password
        val pageNumber = request.pages.firstOrNull() ?: throw IllegalStateException("Incorrect request data!")
        val angle = request.angle
        val guid = URLDecoder.decode(request.guid, "UTF-8")

        val filePath = pathManager.assertPathIsInsideFilesDirectory(guid)

        if (!areFilesSupported(filePath.fileName.toString())) {
            throw InternalServerException("Document types are not supported in sample app, anyway, it is still supported by GroupDocs.Viewer itself. Other probable reason of the error - documents types are different.") // TODO: Need another exception type
        }

        val previewPageWidth = appConfig.viewer.previewPageWidthOrDefault
        val previewPageRatio = appConfig.viewer.previewPageRatioOrDefault

        val pageRotations: Map<Int, Rotation>? = if (filesCache.isEntryExist(guid = guid)) {
            val entry = filesCache.readEntry(guid = guid)

            val entryPage = entry.pages.first { it.pageNumber == pageNumber }

            (angle + entryPage.angle).angleToRotation()?.let { rotation ->
                mapOf(Pair(pageNumber, rotation))
            }
        } else {
            angle.angleToRotation()?.let { rotation ->
                mapOf(Pair(pageNumber, rotation))
            }
        }

        return withContext(Dispatchers.IO) {
            BufferedInputStream(FileInputStream(filePath.toFile())).use { inputStream ->
                var response: RotateResponse? = null
                retrieveLocalFilePagesStream(
                    inputStream = inputStream,
                    password = password,
                    pageRotations = pageRotations,
                    previewWidth = previewPageWidth,
                    previewRatio = previewPageRatio,
                ) { pageNumber, width, height, pageInputStream ->
                    response = RotateResponse(
                        data = String(pageInputStream.readBytes()),
                        angle = 0,
                        number = pageNumber,
                        width = width,
                        height = height,
                    )
                }
                if (filesCache.isEntryExist(guid = guid)) {
                    val entry = filesCache.readEntry(guid = guid)
                    filesCache.createEntry(guid = guid, entry = entry.copy(
                        pages = entry.pages.map {
                            if (it.pageNumber == pageNumber) {
                                it.copy(
                                    angle = pageRotations?.get(pageNumber)?.rotationToAngle() ?: 0,
                                    data = response?.data
                                )
                            } else it
                        }
                    ))
                } else {
                    val entry = MemoryFilesCacheEntry(
                        lastAccess = System.currentTimeMillis(),
                        pages = listOf(
                            MemoryFilesCachePage(
                                pageNumber = pageNumber,
                                angle = pageRotations?.get(pageNumber)?.rotationToAngle() ?: 0,
                                width = response?.width ?: previewPageWidth,
                                height = response?.height ?: (previewPageWidth * previewPageRatio).toInt(),
                                data = response?.data
                            )
                        )
                    )
                    filesCache.createEntry(guid = guid, entry)
                }
                response ?: throw IllegalStateException("Response must not be null")
            }
        }
    }
}