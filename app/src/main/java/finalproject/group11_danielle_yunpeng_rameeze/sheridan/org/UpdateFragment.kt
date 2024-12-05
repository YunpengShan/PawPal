package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentUpdateBinding
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.FeedSchedule
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.PetType
import java.util.Calendar

class UpdateFragment : Fragment() {
    private var _binding: FragmentUpdateBinding? = null
    private val binding get() = _binding!!

    private val args: UpdateFragmentArgs by navArgs()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private var uri: Uri? = null
    private var imageUrl: String? = null
    private lateinit var feedSched: FeedSchedule
    private lateinit var userID: String

    // Declare the ActivityResultLauncher globally
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ActivityResultLauncher in onCreate
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            binding.imgUpdate.setImageURI(uri)
            this.uri = uri
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        userID = firebaseAuth.currentUser?.uid.toString()
        imageUrl = args.petPicURL

        fetchFeedSched()
    }

    private fun setupPetTypeSpinner(selectedType: String) {
        val petTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            PetType.values().map { it.displayName }
        )
        petTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPetTypeUpdate.adapter = petTypeAdapter

        // Pre-select the current type
        val position = PetType.values().indexOfFirst { it.displayName == selectedType }
        if (position >= 0) {
            binding.spinnerPetTypeUpdate.setSelection(position)
        }
    }

    private fun setupDatePicker() {
        binding.edtUpdateVaccinationDates.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Show DatePickerDialog
            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.edtUpdateVaccinationDates.setText(formattedDate)
            }, year, month, day)

            datePickerDialog.show()
        }
    }

    private fun fetchFeedSched() {
        firestore.collection("feedSched").document(args.feedSchedID)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                feedSched = documentSnapshot.toObject(FeedSchedule::class.java)!!
                updateUI()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching feed schedule: ${exception.message}", exception)
            }
    }

    private fun updateUI() {

        binding.apply {
            edtUpdateName.setText(args.name)
            edtUpdateBreed.setText(args.breed)

            edtUpdateVaccinationDates.setText(args.vaccinationDates)

            amUpdateCB.isChecked = feedSched.morning
            noonUpdateCB.isChecked = feedSched.noon
            pmUpdateCB.isChecked = feedSched.night

            edtUpdateFoodAmount.setText(feedSched.amount)
            Picasso.get().load(imageUrl).into(imgUpdate)

            // Setup Spinner and DatePicker
            setupPetTypeSpinner(args.type)
            typeImageView.setImageResource(getDrawableForPetType(args.type!!))

            setupDatePicker()

            btnUpdate.setOnClickListener {
                updateData(userID)
                findNavController().navigate(R.id.action_updateFragment_to_homeFragment)
            }

            imgUpdate.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
        }
    }

    // Helper function to map pet types to drawable resource IDs
    private fun getDrawableForPetType(petType: String): Int {
        return when (petType.uppercase()) {
            "DOG" -> R.drawable.pp_dog
            "CAT" -> R.drawable.pp_cat
            "BIRD" -> R.drawable.pp_bird
            "FISH" -> R.drawable.pp_fish
            "REPTILE" -> R.drawable.pp_reptile
            else -> R.drawable.pets // Fallback image
        }
    }

    private fun updateData(userId: String) {
        val name = binding.edtUpdateName.text.toString()
        val breed = binding.edtUpdateBreed.text.toString()
        val vaccinationDates = binding.edtUpdateVaccinationDates.text.toString()
        val petTypeDisplayName = binding.spinnerPetTypeUpdate.selectedItem.toString()

        val foodAM = binding.amUpdateCB.isChecked
        val foodNoon = binding.noonUpdateCB.isChecked
        val foodPM = binding.pmUpdateCB.isChecked
        val foodAmount = binding.edtUpdateFoodAmount.text.toString()

        if (name.isEmpty()) {
            binding.edtUpdateName.error = "Enter pet name"
            return
        }

        // Map display name to PetType enum
        val petType = PetType.values().find { it.displayName == petTypeDisplayName } ?: return

        val petId = args.id
        val petStoragePath = "images/$userId/$petId.jpg"

        uri?.let {
            //update pet image
            storageRef.child(petStoragePath).putFile(it)
                .addOnSuccessListener { task ->
                    task.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { imageUrl ->

                            // Save Feed Schedule to Firestore
                            val feedSched: MutableMap<String, Any> = hashMapOf(
                                "morning" to foodAM,
                                "noon" to foodNoon,
                                "night" to foodPM,
                                "amount" to foodAmount
                            )
                            firestore.collection("feedSched").document(args.feedSchedID)
                                .update(feedSched)
                                .addOnSuccessListener { fsRef ->
                                    // Save Pet to Firestore w/ newFSID
                                    val pet: MutableMap<String, Any> = hashMapOf(
                                        "ownerID" to userId,
                                        "type" to petType.toString(),
                                        "breed" to breed,
                                        "name" to name,
                                        "vaccinationDates" to vaccinationDates,
                                        "feedSchedID" to args.feedSchedID,
                                        "petPicURL" to imageUrl.toString()
                                    )

                                    firestore.collection("pets").document(petId).set(pet)
                                        .addOnCompleteListener {
                                            showAlertDialog("Pet Updated")
                                        }
                                }
                        }
                }
        } ?: run {
            //no image change
            // Save Feed Schedule to Firestore
            val feedSched: MutableMap<String, Any> = hashMapOf(
                "morning" to foodAM,
                "noon" to foodNoon,
                "night" to foodPM,
                "amount" to foodAmount
            )
            firestore.collection("feedSched").document(args.feedSchedID)
                .update(feedSched)
                .addOnSuccessListener { fsRef ->

                    // Save Pet to Firestore w/ newFSID
                    val pet: MutableMap<String, Any> = hashMapOf(
                        "ownerID" to userId,
                        "type" to petType.toString(),
                        "breed" to breed,
                        "name" to name,
                        "vaccinationDates" to vaccinationDates,
                        "feedSchedID" to args.feedSchedID,
                        "petPicURL" to imageUrl.toString()
                    )

                    firestore.collection("pets").document(petId).set(pet)
                        .addOnCompleteListener {
                            showAlertDialog("Pet Updated")
                        }
                }
        }
    }

    private fun showAlertDialog(message: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", binding.edtUpdateName.text.toString())
        outState.putString("breed", binding.edtUpdateBreed.text.toString())
        outState.putString("vaccinationDates", binding.edtUpdateVaccinationDates.text.toString())
        outState.putString("foodAmount", binding.edtUpdateFoodAmount.text.toString())
        outState.putBoolean("foodAM", binding.amUpdateCB.isChecked)
        outState.putBoolean("foodNoon", binding.noonUpdateCB.isChecked)
        outState.putBoolean("foodPM", binding.pmUpdateCB.isChecked)
        outState.putInt("petTypePosition", binding.spinnerPetTypeUpdate.selectedItemPosition)
        outState.putParcelable("uri", uri)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            binding.edtUpdateName.setText(it.getString("name"))
            binding.edtUpdateBreed.setText(it.getString("breed"))
            binding.edtUpdateVaccinationDates.setText(it.getString("vaccinationDates"))
            binding.edtUpdateFoodAmount.setText(it.getString("foodAmount"))
            binding.amUpdateCB.isChecked = it.getBoolean("foodAM")
            binding.noonUpdateCB.isChecked = it.getBoolean("foodNoon")
            binding.pmUpdateCB.isChecked = it.getBoolean("foodPM")
            binding.spinnerPetTypeUpdate.setSelection(it.getInt("petTypePosition"))
            uri = it.getParcelable("uri")
            uri?.let { imageUri ->
                binding.imgUpdate.setImageURI(imageUri)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
