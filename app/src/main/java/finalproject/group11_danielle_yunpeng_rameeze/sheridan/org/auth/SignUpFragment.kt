package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        dbUsers = FirebaseFirestore.getInstance()
        fbStorage = FirebaseStorage.getInstance()

        // Profile picture picker
        binding.profileImageView.setOnClickListener {
            openImagePicker()
        }

        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty() && profilePicUri.toString().isNotEmpty()) {
                signUpUser(email, password)
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
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
                            // Handle further actions if needed, like updating the UI or database
                        }
                        .addOnFailureListener { exception ->
                            // Handle failure in retrieving the download URL
                            Log.e("UploadProfilePicture", "Failed to get download URL", exception)
                        }
                }
                .addOnFailureListener { exception ->
                    // Handle failure in uploading the file
                    Log.e("UploadProfilePicture", "Failed to upload profile picture", exception)
                }
        }
    }

    private fun signUpUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //get user id
                    userid = auth.currentUser!!.uid
                    //upload profile pic and set pic url
                    uploadProfilePictureToStorage()


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
                            Toast.makeText(context, "Sign-Up Successful", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)

                        }
                } else {
                    Toast.makeText(context, "Sign-Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
