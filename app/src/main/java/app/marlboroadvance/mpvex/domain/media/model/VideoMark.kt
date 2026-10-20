package app.marlboroadvance.mpvex.domain.media.model

/** Explicit library labels. Null means the normal, automatically calculated playback status. */
enum class VideoMark {
  LAST_PLAYED,
  NEW,
  FINISHED,
  NONE;

  companion object {
    fun fromStored(value: String?): VideoMark? = entries.firstOrNull { it.name == value }
  }
}
