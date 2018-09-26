package com.kwai.feedexposure;

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.graphics.Rect
import android.os.SystemClock
import android.support.annotation.UiThread
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.*
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.Log

class PhotoExposureCalculator: LifecycleObserver {

  companion object {
    private const val TAG = "PhotoExposureCalculator"
    private const val DEBUG = false
    private const val THROTTLE_INTERVAL_MS = 250 // ms
  }

  val onScrollListener : OnScrollListener = InternalOnScrollListener()

  private val exposureDuration = HashMap<String, Long>()
  private val firstShownTimestamp = HashMap<String, Long>()
  private val visibleRect = Rect()
  private var pauseTime = SystemClock.elapsedRealtime()
  private var lastCalculateTime = 0L

  inner class InternalOnScrollListener : OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
      // This method cost too much time for one frame (2~10ms on average),
      // we can add some strategy to reduce calling times.
      // Calculate 1000/THROTTLE_INTERVAL_MS times per second.
      if (recyclerView.scrollState == SCROLL_STATE_IDLE) {
        updatePhotoExposureDuration(recyclerView)
      } else {
        if (SystemClock.elapsedRealtime() - lastCalculateTime > THROTTLE_INTERVAL_MS) {
          updatePhotoExposureDuration(recyclerView)
        }
      }
    }
  }

  private fun updatePhotoExposureDuration(recyclerView: RecyclerView) {
    val layoutManager = recyclerView.layoutManager
    val adapter = recyclerView.adapter as MainActivity.MyAdapter

    lastCalculateTime = SystemClock.elapsedRealtime()
    return when (layoutManager) {
      is StaggeredGridLayoutManager -> {
        val span = layoutManager.spanCount
        val firstVisiblePositions = IntArray(span)
        val lastVisiblePositions = IntArray(span)
        layoutManager.findFirstVisibleItemPositions(firstVisiblePositions)
        layoutManager.findLastVisibleItemPositions(lastVisiblePositions)

        val minPos = firstVisiblePositions.filter { it != NO_POSITION }.min() ?: NO_POSITION
        val maxPos = lastVisiblePositions.filter { it != NO_POSITION }.max() ?: NO_POSITION

        if (minPos != NO_POSITION) {
          // Elements in [minPos, maxPos] may be visible photos,
          // we have to check visible rect to make sure it's really visible.
          if (DEBUG) Log.d(TAG, "Candidates: [$minPos, $maxPos]")
          for (i in minPos..maxPos) {
            val photo = adapter.getItem(i)
            val view = layoutManager.findViewByPosition(i) // O(n)

            val visible = view.getLocalVisibleRect(visibleRect)
            if (!visible) continue
            val visibleHeight = visibleRect.bottom - visibleRect.top
            view.getDrawingRect(visibleRect)
            val fullHeight = visibleRect.bottom
            val atLeastHalfVisible = visibleHeight * 2 >= fullHeight
            val key = photo.photoId

            // Currently visible or newly visible element.
            if (atLeastHalfVisible) {
              if (key in firstShownTimestamp) {
                if (DEBUG) Log.d(TAG, "Feed $key is still visible, just keep it.")
              } else {
                firstShownTimestamp[key] = SystemClock.elapsedRealtime()
                if (DEBUG) Log.d(TAG, "Add newly shown $key.")
              }
            } else {
              // Not visible yet or fading away.
              if (key in firstShownTimestamp) {
                // out of view range
                if (DEBUG) Log.d(TAG, "Remove invisible feed $key")
                val timestamp = firstShownTimestamp.remove(key)!!
                exposureDuration[photo.photoId] = SystemClock.elapsedRealtime() - timestamp
                + (exposureDuration[photo.photoId] ?: 0)
              } else {
                if (DEBUG) Log.d(TAG, "Feed $key is still not visible, keep ignoring it.")
              }
            }
          }
        } else {
          // minPos == NO_POSITION if and only if maxPox == NO_POSITION
          // There isn't any photos. Just ignore it.
        }
      }
      else ->
        throw UnsupportedOperationException("Unsupported layout manager.")
    }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  fun pause() {
    if (DEBUG) Log.d(TAG, "Pause feed exposure calculator")
    check(pauseTime == 0L) { "Calculator is already paused" }
    pauseTime = SystemClock.elapsedRealtime()
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  fun resume() {
    if (DEBUG) Log.d(TAG, "Resume feed exposure calculator")
    check(pauseTime != 0L) { "Calculator is already resumed at $pauseTime" }
    val delta = SystemClock.elapsedRealtime() - pauseTime

    for ((k, v) in firstShownTimestamp) {
      firstShownTimestamp[k] = v + delta
    }
    pauseTime = 0
  }

  @UiThread
  fun pollFeedExposuresDurations(): HashMap<String, Long> {
    flushCurrentShowingFeeds()
    val durations = HashMap(exposureDuration)
    exposureDuration.clear()
    return durations
  }

  private fun flushCurrentShowingFeeds() {
    for ((k, v) in firstShownTimestamp) {
      if (k in exposureDuration) {
        exposureDuration[k] = (exposureDuration[k] ?: 0) + SystemClock.elapsedRealtime() - v
      }
    }
  }
}
