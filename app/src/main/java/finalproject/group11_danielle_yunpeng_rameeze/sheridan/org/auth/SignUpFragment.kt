package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log // Keeping log messages
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog // For dialogs
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.R
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentSignUpBinding

class SignUpFragment : Fragment() {
    private val PICK_IMAGE_REQUEST = 1
    private var profilePicUri: Uri? = null

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var dbUsers: FirebaseFirestore
    private lateinit var fbStorage: FirebaseStorage
    private lateinit var userid: String
    private var picurl = ""
    private val viewModel: SignUpViewModel by viewModels() // ViewModel for data persistence

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        dbUsers = FirebaseFirestore.getInstance()
        fbStorage = FirebaseStorage.getInstance()

        // Restore data from ViewModel
        binding.etName.setText(viewModel.name)
        binding.etEmail.setText(viewModel.email)
        binding.etPassword.setText(viewModel.password)
        binding.etPhone.setText(viewModel.phone)
        viewModel.profilePicUriString?.let {
            profilePicUri = Uri.parse(it)
            binding.profileImageView.setImageURI(profilePicUri)
        }

        // Profile picture picker
        binding.profileImageView.setOnClickListener {
            openImagePicker()
        }

        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty() && profilePicUri != null) {
                signUpUser(email, password)
            } else {
                showAlertDialog("Warning", "Please fill all fields")
            }
        }

        binding.tvSignInPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }

        return binding.root
    }


    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && data != null && data.data != null) {
            profilePicUri = data.data
            binding.profileImageView.setImageURI(profilePicUri)
        }
    }

    private fun uploadProfilePictureToStorage(){
        profilePicUri?.let { uri -> // Use Kotlin's safe call operator
            val storageRef = fbStorage.reference.child("userImages/$userid.jpg")
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    // File upload successful, now retrieve the download URL
                    storageRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            picurl = downloadUri.toString()
                            Log.d("UploadProfilePicture", "Download URL: $picurl")
                            // After obtaining the download URL, proceed to save user data
                            saveUserData()
                        }
                        .addOnFailureListener { exception ->
                            // Handle failure in retrieving the download URL
                            Log.e("UploadProfilePicture", "Failed to get download URL", exception)
                            showAlertDialog("Error", "Failed to get profile picture URL.")
                        }
                }
                .addOnFailureListener { exception ->
                    // Handle failure in uploading the file
                    Log.e("UploadProfilePicture", "Failed to upload profile picture", exception)
                    showAlertDialog("Error", "Failed to upload profile picture.")
                }
        } ?: run {
            // If profilePicUri is null, proceed to save user data without picture
            saveUserData()
        }
    }

    private fun signUpUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get user ID
                    userid = auth.currentUser!!.uid
                    // Upload profile pic and set pic URL, then save user data
                    uploadProfilePictureToStorage()
                } else {
                    showAlertDialog("Sign-Up Failed", task.exception?.message ?: "Unknown error")
                }
            }
    }

    private fun saveUserData() {
        val user: MutableMap<String, Any> = HashMap()
        user["name"] = binding.etName.text.toString()
        user["NumPets"] = 0
        user["email"] = binding.etEmail.text.toString()
        user["phone"] = binding.etPhone.text.toString()
        user["picUrl"] = picurl

        dbUsers.collection("users")
            .document(userid)
            .set(user)
            .addOnSuccessListener {
                showAlertDialog("Success", "Sign-Up Successful")
                findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
            }
            .addOnFailureListener { exception ->
                Log.e("SaveUserData", "Failed to save user data", exception)
                showAlertDialog("Error", "Failed to save user data.")
            }
    }

    // Updated function to accept title and message
    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Save data to ViewModel before destroying the view
        viewModel.name = binding.etName.text.toString()
        viewModel.email = binding.etEmail.text.toString()
        viewModel.password = binding.etPassword.text.toString()
        viewModel.phone = binding.etPhone.text.toString()
        viewModel.profilePicUriString = profilePicUri?.toString()
        _binding = null // Avoid memory leaks by setting binding to null
    }

}
