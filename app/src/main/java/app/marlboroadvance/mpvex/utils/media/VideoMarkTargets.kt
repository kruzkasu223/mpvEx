package app.marlboroadvance.mpvex.utils.media

import android.content.Context
import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Resolve nested and overlapping folder selections once, without a depth limit or silent failures. */
suspend fun collectVideosForMarking(context: Context, folderPaths: List<String>): List<Video> =
  withContext(Dispatchers.IO) {
    val pending = ArrayDeque(folderPaths)
    val visited = mutableSetOf<String>()
    val videos = linkedMapOf<String, Video>()
    while (pending.isNotEmpty()) {
      currentCoroutineContext().ensureActive()
      val path = pending.removeFirst()
      if (!visited.add(File(path).canonicalPath)) continue
      MediaFileRepository.scanDirectory(context, path, useFastCount = true).getOrThrow().forEach { item ->
        when (item) {
          is FileSystemItem.VideoFile -> videos[item.video.path] = item.video
          is FileSystemItem.Folder -> pending.addLast(item.path)
        }
      }
    }
    videos.values.toList()
  }
