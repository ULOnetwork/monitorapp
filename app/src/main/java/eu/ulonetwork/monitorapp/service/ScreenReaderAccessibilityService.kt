package eu.ulonetwork.monitorapp.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import eu.ulonetwork.monitorapp.data.RuleEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reads the text currently visible on screen (across apps) by walking the active window's
 * accessibility node tree, and hands it off to [RuleEvaluator] for keyword matching.
 *
 * Text extraction is debounced by [DEBOUNCE_MILLIS] so that rapid successive events (e.g. while
 * scrolling or typing) only trigger a single evaluation pass.
 *
 * Evaluation is primarily triggered by accessibility events, but some apps update their screen
 * without dispatching a window-state/content-changed event (e.g. custom-drawn views, or updates
 * while the screen is off), which would otherwise leave a stale ISSUE/RESOLVED state undetected
 * indefinitely. [pollRunnable] re-evaluates every [POLL_INTERVAL_MILLIS] regardless of events as
 * a fallback safety net.
 */
class ScreenReaderAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingEvaluation: Runnable? = null
    private var currentEvaluationJob: Job? = null

    private lateinit var ruleEvaluator: RuleEvaluator

    private val pollRunnable = object : Runnable {
        override fun run() {
            scheduleEvaluation(null)
            mainHandler.postDelayed(this, POLL_INTERVAL_MILLIS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ruleEvaluator = RuleEvaluator(applicationContext)
        mainHandler.postDelayed(pollRunnable, POLL_INTERVAL_MILLIS)
        Log.i(TAG, "ScreenReaderAccessibilityService verbonden")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> scheduleEvaluation(event.packageName?.toString())
        }
    }

    private fun scheduleEvaluation(eventPackageName: String?) {
        pendingEvaluation?.let { mainHandler.removeCallbacks(it) }

        val runnable = Runnable {
            currentEvaluationJob?.cancel()
            currentEvaluationJob = serviceScope.launch {
                extractAndEvaluate(eventPackageName)
            }
        }
        pendingEvaluation = runnable
        mainHandler.postDelayed(runnable, DEBOUNCE_MILLIS)
    }

    private suspend fun extractAndEvaluate(eventPackageName: String?) {
        val root = rootInActiveWindow
        val appPackage = eventPackageName ?: root?.packageName?.toString() ?: return

        // Deliberately evaluate even when no text is found (root missing, or a genuinely blank/
        // sparse screen): a NOT_CONTAINS rule is meant to fire exactly when its keyword is absent,
        // which includes the case where there's little or no text on screen at all. Skipping
        // evaluation on blank text would silently prevent NOT_CONTAINS rules from ever triggering
        // in their most common use case.
        val screenText = if (root != null) {
            val builder = StringBuilder()
            try {
                collectText(root, builder)
            } finally {
                root.recycle()
            }
            builder.toString()
        } else {
            ""
        }

        ruleEvaluator.evaluate(appPackage, screenText)
    }

    /**
     * Recursively walks [node] and its children, appending any non-empty text or content
     * description to [out]. Child node infos obtained via [AccessibilityNodeInfo.getChild] are
     * recycled after use to avoid leaking native resources.
     */
    private fun collectText(node: AccessibilityNodeInfo, out: StringBuilder) {
        val text = node.text
        if (!text.isNullOrBlank()) {
            out.append(text).append('\n')
        }
        val description = node.contentDescription
        if (!description.isNullOrBlank()) {
            out.append(description).append('\n')
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            try {
                collectText(child, out)
            } finally {
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "ScreenReaderAccessibilityService onderbroken")
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingEvaluation?.let { mainHandler.removeCallbacks(it) }
        mainHandler.removeCallbacks(pollRunnable)
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "ScreenReaderService"
        private const val DEBOUNCE_MILLIS = 800L
        private const val POLL_INTERVAL_MILLIS = 60_000L
    }
}
