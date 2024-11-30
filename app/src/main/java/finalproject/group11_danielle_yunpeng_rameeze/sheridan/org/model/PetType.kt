package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model

enum class PetType(val displayName: String) {
    CAT("Cat"),
    DOG("Dog"),
    BIRD("Bird"),
    REPTILE("Reptile"),
    FISH("Fish");

    override fun toString(): String {
        return displayName
    }
}
