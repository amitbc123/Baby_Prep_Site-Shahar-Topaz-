package com.oryareach.feature.calendar

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryareach.core.database.repository.CycleRepository
import com.oryareach.core.database.repository.ImportantDateRepository
import com.oryareach.core.database.repository.TaskRepository
import com.oryareach.core.domain.cycle.calculateCycleStatistics
import com.oryareach.core.domain.cycle.predictNextCycle
import com.oryareach.core.model.EntityType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Stable
interface CalendarActions {
    fun onPreviousMonth()
    fun onNextMonth()
    fun onSelectDate(date: LocalDate)
    fun onDismissDaySheet()
    fun onEventClick(event: CalendarEvent)
    fun onRefresh()
}

/**
 * Merges tasks (by due date), important dates, and logged/predicted periods into one set of
 * [CalendarEvent]s. Deliberately not a separate persisted table — like [CalendarEffect], this
 * is a read-side view over three existing repositories, recomputed from their live flows via
 * `combine` rather than written anywhere, so it can never go stale independently of them.
 */
class CalendarViewModel(
    private val tasks: TaskRepository,
    private val importantDates: ImportantDateRepository,
    private val cycles: CycleRepository,
    private val syncEngine: com.oryareach.core.sync.SyncEngine,
    private val workspaceId: () -> String?,
) : ViewModel(), CalendarActions {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val _uiState = MutableStateFlow(CalendarUiState(visibleMonth = today.startOfMonth()))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CalendarEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        workspaceId()?.let { id ->
            viewModelScope.launch {
                combine(
                    tasks.observeAll(id),
                    importantDates.observeAll(id),
                    cycles.observeAll(id),
                ) { taskList, dateList, cycleList ->
                    val taskEvents = taskList.mapNotNull { task ->
                        task.dueDate?.let {
                            CalendarEvent(it, EntityType.TASK, task.id, task.title, CalendarEventKind.TASK_DUE)
                        }
                    }
                    val dateEvents = dateList.map { date ->
                        CalendarEvent(date.date, EntityType.IMPORTANT_DATE, date.id, date.title, CalendarEventKind.IMPORTANT_DATE)
                    }
                    val actualEvents = cycleList.flatMap { cycle ->
                        val end = cycle.endDate ?: cycle.startDate
                        daysBetween(cycle.startDate, end).map {
                            CalendarEvent(it, EntityType.CYCLE, cycle.id, "", CalendarEventKind.PERIOD_ACTUAL)
                        }
                    }
                    val prediction = predictNextCycle(cycleList)
                    val predictedEvents = prediction.nextPeriodStart?.let { start ->
                        val periodLength = calculateCycleStatistics(cycleList).averagePeriodLengthDays ?: DEFAULT_PERIOD_LENGTH_DAYS
                        val end = start.plus(periodLength - 1, DateTimeUnit.DAY)
                        daysBetween(start, end).map {
                            CalendarEvent(it, EntityType.CYCLE, "predicted", "", CalendarEventKind.PERIOD_PREDICTED)
                        }
                    }.orEmpty()

                    taskEvents + dateEvents + actualEvents + predictedEvents
                }.collect { events -> set { it.copy(events = events) } }
            }
        }
    }

    override fun onPreviousMonth() = shiftMonth(-1)
    override fun onNextMonth() = shiftMonth(1)

    private fun shiftMonth(delta: Int) = set { it.copy(visibleMonth = it.visibleMonth.plus(delta, DateTimeUnit.MONTH)) }

    override fun onSelectDate(date: LocalDate) = set { it.copy(selectedDate = date) }
    override fun onDismissDaySheet() = set { it.copy(selectedDate = null) }

    override fun onEventClick(event: CalendarEvent) {
        _effects.trySend(CalendarEffect.OpenEvent(event))
    }

    override fun onRefresh() {
        if (_uiState.value.refreshing) return
        set { it.copy(refreshing = true) }
        viewModelScope.launch {
            syncEngine.sync()
            set { it.copy(refreshing = false) }
        }
    }

    private fun set(block: (CalendarUiState) -> CalendarUiState) {
        _uiState.value = block(_uiState.value)
    }

    private companion object {
        const val DEFAULT_PERIOD_LENGTH_DAYS = 5
    }
}

private fun LocalDate.startOfMonth(): LocalDate = LocalDate(year, month, 1)

private fun daysBetween(start: LocalDate, end: LocalDate): List<LocalDate> {
    val days = mutableListOf<LocalDate>()
    var d = start
    while (d <= end) {
        days += d
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return days
}
