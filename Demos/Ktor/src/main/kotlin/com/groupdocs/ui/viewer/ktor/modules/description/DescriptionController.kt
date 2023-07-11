package com.groupdocs.ui.viewer.ktor.modules.description

import com.groupdocs.ui.viewer.ktor.cache.FilesCache
import com.groupdocs.ui.viewer.ktor.cache.MemoryFilesCacheEntry
import com.groupdocs.ui.viewer.ktor.cache.MemoryFilesCachePage
import com.groupdocs.ui.viewer.ktor.manager.PathManager
import com.groupdocs.ui.viewer.ktor.model.DescriptionEntity
import com.groupdocs.ui.viewer.ktor.model.DescriptionRequest
import com.groupdocs.ui.viewer.ktor.model.PageDescriptionEntity
import com.groupdocs.ui.viewer.ktor.modules.BaseController
import com.groupdocs.ui.viewer.ktor.usecase.RetrieveLocalFilePagesDataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.net.URLDecoder
import java.net.URLEncoder

class DescriptionControllerImpl(
    private val retrieveLocalFilePagesData: RetrieveLocalFilePagesDataUseCase,
    private val pathManager: PathManager,
    private val filesCache: FilesCache
) : BaseController(), DescriptionController, KoinComponent {
    override suspend fun description(request: DescriptionRequest): DescriptionEntity {
        val guid = URLDecoder.decode(request.guid, "UTF-8")
        val path = pathManager.assertPathIsInsideFilesDirectory(guid)
        val password = request.password
        val previewPageWidth = appConfig.viewer.previewPageWidthOrDefault
        val previewPageRatio = appConfig.viewer.previewPageRatioOrDefault

        val entity = DescriptionEntity(
            guid = URLEncoder.encode(guid, "UTF-8"),
            printAllowed = appConfig.common.print
        )
        if (filesCache.isEntryExist(guid = guid)) {
            val memoryFilesCacheEntry = filesCache.readEntry(guid = guid)
            entity.pages.addAll(memoryFilesCacheEntry.pages.map { mapEntry ->
                PageDescriptionEntity(
                    number = mapEntry.pageNumber,
                    width = previewPageWidth,
                    height = (previewPageWidth * previewPageRatio).toInt(),
                    data = mapEntry.data
                )
            })
            return entity
        } else {
            return withContext(Dispatchers.IO) {
                BufferedInputStream(FileInputStream(path.toFile())).use { inputStream ->
                    retrieveLocalFilePagesData(
                        inputStream = inputStream,
                        password = password,
                        previewWidth = previewPageWidth,
                        previewRatio = previewPageRatio,
                    ) { pageNumber, width, height, pageInputStream ->
                        val data = String(pageInputStream.readBytes())
                        entity.pages.add(
                            PageDescriptionEntity(
                                number = pageNumber,
                                width = width,
                                height = height,
                                data = data
                            )
                        )
                    }
                    filesCache.createEntry(guid = guid, entry = MemoryFilesCacheEntry(
                        pages = entity.pages.map { page ->
                            MemoryFilesCachePage(
                                pageNumber = page.number,
                                angle = page.angle,
                                width = page.width,
                                height = page.height,
                                data = page.data
                            )
                        }
                    ))
                    entity
                }
            }
        }
    }

}

interface DescriptionController {
    suspend fun description(request: DescriptionRequest): DescriptionEntity
}