package io.github.bric3.fireplace.views.cpu

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.byThreads
import io.github.bric3.fireplace.stacktraceTreeModel
import io.github.bric3.fireplace.ui.autoSize
import io.github.bric3.fireplace.views.FlameGraphPane
import io.github.bric3.fireplace.views.ViewPanel
import org.openjdk.jmc.common.item.IItem
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.common.item.ItemCollectionToolkit
import org.openjdk.jmc.flightrecorder.JfrAttributes
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JSplitPane

class MethodCpuSample(private val jfrBinder: JFRBinder) : ViewPanel {
    private var executionSample: IItemCollection = ItemCollectionToolkit.EMPTY
    private var threadMapping: Map<String, List<IItem>> = mapOf()
    override val identifier = "CPU"

    override val view by lazy {
        val flameGraphPane = FlameGraphPane()

        val threadListModel = DefaultListModel<String>()
        val imcThreadJList = object : JList<String>(threadListModel) {
            override fun processMouseEvent(e: MouseEvent) {
                // Clear selection on clicking in empty space
                if ((e.id == MouseEvent.MOUSE_CLICKED || e.id == MouseEvent.MOUSE_PRESSED) &&
                    !this.getCellBounds(0, this.model.size - 1).contains(e.point)
                ) {
                    this.clearSelection()
                    e.consume()
                } else {
                    super.processMouseEvent(e)
                }
            }
        }.apply {
            addListSelectionListener {
                flameGraphPane.setStacktraceTreeModel(
                    when (this.selectedValue) {
                        ALL_THREADS_LABEL, null -> executionSample.stacktraceTreeModel()
                        else -> selectedIndices.map { threadListModel[it] }
                            .mapNotNull { threadMapping[it] }
                            .flatten()
                            .stacktraceTreeModel()
                    }
                )
            }
        }


        jfrBinder.bindEvents(
            JfrAnalyzer::executionSamples
        ) {
            executionSample = it
            threadMapping = byThreads(
                executionSample,
                JfrAttributes.EVENT_THREAD,
                JdkAttributes.EVENT_THREAD_NAME
            )
            threadListModel.run {
                clear()
                addElement(ALL_THREADS_LABEL)
                addAll(threadMapping.keys.sortedWith(
                    Comparator.nullsLast(
                        Comparator.naturalOrder()
                    )
                ))
            }

            flameGraphPane.setStacktraceTreeModel(
                executionSample.stacktraceTreeModel(
                    //JdkAttributes.SAMPLE_WEIGHT
                )
            )
        }


        JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imcThreadJList, flameGraphPane).apply {
            autoSize(0.2)
        }
    }

    companion object {
        private const val ALL_THREADS_LABEL = "<html><b>All threads</b></html>"
    }
}