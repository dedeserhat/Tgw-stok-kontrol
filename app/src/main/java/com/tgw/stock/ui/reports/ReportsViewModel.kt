package com.tgw.stock.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.repository.ReportsData
import com.tgw.stock.data.repository.ReportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class ReportPeriod { TODAY, LAST_7_DAYS, LAST_30_DAYS, CUSTOM }

class ReportsViewModel(private val reportsRepository: ReportsRepository) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.LAST_7_DAYS)
    val period: StateFlow<ReportPeriod> = _period.asStateFlow()

    private val _customFrom = MutableStateFlow<Long?>(null)
    private val _customTo = MutableStateFlow<Long?>(null)

    private val _data = MutableStateFlow<ReportsData?>(null)
    val data: StateFlow<ReportsData?> = _data.asStateFlow()

    init { load() }

    fun setPeriod(p: ReportPeriod) {
        _period.value = p
        if (p != ReportPeriod.CUSTOM) load()
    }

    fun setCustomRange(from: Long, to: Long) {
        _customFrom.value = from
        _customTo.value = to
        _period.value = ReportPeriod.CUSTOM
        load()
    }

    private fun rangeFor(period: ReportPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (period) {
            ReportPeriod.TODAY -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to now
            }
            ReportPeriod.LAST_7_DAYS -> (now - TimeUnit.DAYS.toMillis(7)) to now
            ReportPeriod.LAST_30_DAYS -> (now - TimeUnit.DAYS.toMillis(30)) to now
            ReportPeriod.CUSTOM -> (_customFrom.value ?: now) to (_customTo.value ?: now)
        }
    }

    private fun load() {
        viewModelScope.launch {
            val (from, to) = rangeFor(_period.value)
            _data.value = reportsRepository.getReport(from, to)
        }
    }
}
