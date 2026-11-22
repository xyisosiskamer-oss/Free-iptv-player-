package com.example.data.model

data class EpgProgramUi(
    val title: String,
    val description: String?,
    val category: String?,
    val startTimeMillis: Long,
    val endTimeMillis: Long
) {
    fun progressAt(nowMillis: Long): Float {
        val duration = (endTimeMillis - startTimeMillis).coerceAtLeast(1L)
        return ((nowMillis - startTimeMillis).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
}

data class ChannelItem(
    val id: Long,
    val playlistId: Long,
    val playlistName: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupTitle: String,
    val tvgId: String?,
    val userAgent: String?,
    val referrer: String?,
    val position: Int,
    val isFavorite: Boolean,
    val catchupType: String? = null,
    val catchupDays: Int = 0,
    val catchupSource: String? = null,
    val currentProgram: EpgProgramUi? = null,
    val nextProgram: EpgProgramUi? = null,
    val schedule: List<EpgProgramUi> = emptyList(),
    val archivePrograms: List<EpgProgramUi> = emptyList()
) {
    val hasCatchup: Boolean
        get() = catchupDays > 0 || !catchupType.isNullOrBlank() || !catchupSource.isNullOrBlank()
}
