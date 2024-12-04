package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.R
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.SignInViewModel
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val viewModel: SignInViewModel by viewModels() // ViewModel for data persistence

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        // Restore data from ViewModel
        binding.etEmail.setText(viewModel.email)
        binding.etPassword.setText(viewModel.password)

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                signInUser(email, password)
            } else {
                showAlertDialog("Warning", "Please fill all fields")
            }
        }

        binding.tvSignUpPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Save data to ViewModel before destroying the view
        viewModel.email = binding.etEmail.text.toString()
        viewModel.password = binding.etPassword.text.toString()
        _binding = null // Avoid memory leaks by setting binding to null
    }

    private fun signInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
                } else {
                    showAlertDialog("Sign-In Failed", task.exception?.message ?: "Unknown error")
                }
            }
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
