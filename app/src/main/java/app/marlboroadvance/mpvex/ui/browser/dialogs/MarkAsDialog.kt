package app.marlboroadvance.mpvex.ui.browser.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.database.repository.VideoMarkRepository
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoMark
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun videoMarkLabel(mark: VideoMark): String = stringResource(when (mark) {
  VideoMark.LAST_PLAYED -> R.string.mark_last_played
  VideoMark.NEW -> R.string.video_label_new
  VideoMark.FINISHED -> R.string.mark_finished
  VideoMark.NONE -> R.string.mark_none
})

/** Shared action for video, folder and mixed selections. Capture targets before starting IO. */
@Composable
fun rememberMarkAsAction(
  getVideos: suspend () -> List<Video>,
  onComplete: () -> Unit,
): () -> Unit {
  val repository = koinInject<VideoMarkRepository>()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val currentGetVideos by rememberUpdatedState(getVideos)
  val currentOnComplete by rememberUpdatedState(onComplete)
  var open by remember { mutableStateOf(false) }
  var busy by remember { mutableStateOf(false) }
  var saving by remember { mutableStateOf(false) }
  var loadJob by remember { mutableStateOf<Job?>(null) }
  var targets by remember { mutableStateOf<List<Video>>(emptyList()) }
  var commonMark by remember { mutableStateOf<VideoMark?>(null) }
  val loadError = stringResource(R.string.mark_load_error)
  val saveError = stringResource(R.string.mark_save_error)
  val emptyMessage = stringResource(R.string.mark_no_videos)
  val successMessage = stringResource(R.string.mark_saved)

  if (open) {
    AlertDialog(
      onDismissRequest = {
        if (!saving) { loadJob?.cancel(); open = false }
      },
      title = { Text(stringResource(R.string.mark_as)) },
      text = {
        Column(Modifier.fillMaxWidth().selectableGroup()) {
          if (busy) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
              CircularProgressIndicator()
            }
            Text(
              stringResource(if (saving) R.string.mark_saving else R.string.mark_loading),
              modifier = Modifier.padding(top = 16.dp),
              style = MaterialTheme.typography.bodyMedium,
            )
          } else {
            VideoMark.entries.forEach { mark ->
              Row(
                modifier = Modifier.fillMaxWidth()
                  .selectable(selected = commonMark == mark, role = Role.RadioButton, onClick = {
                    busy = true
                    saving = true
                    scope.launch {
                      try {
                        repository.mark(targets, mark)
                        open = false
                        currentOnComplete()
                        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                      } catch (e: CancellationException) {
                        throw e
                      } catch (e: Exception) {
                        android.util.Log.e("MarkAsDialog", "Could not save video marks", e)
                        Toast.makeText(context, saveError, Toast.LENGTH_LONG).show()
                      } finally {
                        busy = false
                        saving = false
                      }
                    }
                  }).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(selected = commonMark == mark, onClick = null)
                Text(videoMarkLabel(mark), Modifier.padding(start = 16.dp),
                  style = MaterialTheme.typography.bodyLarge)
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { loadJob?.cancel(); open = false }, enabled = !saving) {
          Text(stringResource(R.string.generic_cancel))
        }
      },
    )
  }

  return {
    if (!open) {
      open = true
      busy = true
      loadJob = scope.launch {
        try {
          // The provider snapshots the selection on Main; filesystem scans run on IO inside it.
          targets = currentGetVideos().distinctBy { it.path }
          if (targets.isEmpty()) {
            open = false
            Toast.makeText(context, emptyMessage, Toast.LENGTH_SHORT).show()
          } else {
            commonMark = repository.commonMark(targets)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          open = false
          android.util.Log.e("MarkAsDialog", "Could not load selected videos", e)
          Toast.makeText(context, loadError, Toast.LENGTH_LONG).show()
        } finally {
          busy = false
        }
      }
    }
  }
}
