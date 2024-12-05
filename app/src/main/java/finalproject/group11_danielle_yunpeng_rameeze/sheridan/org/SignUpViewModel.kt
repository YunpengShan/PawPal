package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.auth

import androidx.lifecycle.ViewModel

class SignUpViewModel : ViewModel() {
    var name: String = ""
    var email: String = ""
    var password: String = ""
    var phone: String = ""
    var profilePicUriString: String? = null
}
