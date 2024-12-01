package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model

data class Pets(
    val id: String? = null,
    val type: String? = null,
    val breed: String? = null,
    val name: String? = null,
    val vaccinationDates: String? = null,
    val foodAmount: String? = null,
    val petPicURL: String? = null
) {
    constructor() : this(null, null, null, null, null, null, null)
}
