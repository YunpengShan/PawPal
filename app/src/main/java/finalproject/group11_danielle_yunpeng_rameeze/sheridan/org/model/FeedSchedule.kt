package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model

data class FeedSchedule(
    val id: String? = null,
    val morning: Boolean = false,
    val noon: Boolean = false,
    val night: Boolean = false,
    val amount: String? = null
)
