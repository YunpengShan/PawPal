package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class AboutFragment : Fragment() {

    private lateinit var tvAppDetails: TextView
    private lateinit var tvStudentInfo: TextView
    private lateinit var tvAppPurpose: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        // Initialize the UI components
        tvAppDetails = view.findViewById(R.id.tvAppDetails)
        tvStudentInfo = view.findViewById(R.id.tvStudentInfo)
        tvAppPurpose = view.findViewById(R.id.tvAppPurpose)

        // Set text for the about section
        tvAppDetails.text = "App Details: This is a pet management app."
        tvStudentInfo.text = "Student Information:\nYunpeng Shan\nDanielle Creary-Thomas\nRameez Arshad"
        tvAppPurpose.text = "Purpose of the App: This app is designed to help users manage their pets by tracking their details and information."

        return view
    }
}
