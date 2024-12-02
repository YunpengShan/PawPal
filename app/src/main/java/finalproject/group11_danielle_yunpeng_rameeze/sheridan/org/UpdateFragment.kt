package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentUpdateBinding
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.FeedSchedule
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.PetType
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.Pets
import java.util.Calendar

class UpdateFragment : Fragment() {
    private var _binding: FragmentUpdateBinding? = null
    private val binding get() = _binding!!

    private val args: UpdateFragmentArgs by navArgs()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private var uri: Uri? = null
    private var imageUrl: String? = null
    private lateinit var feedSched:FeedSchedule

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

        val userId = firebaseAuth.currentUser?.uid

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
            binding.imgUpdate.setImageURI(it)
            uri = it
        }
        imageUrl = args.petPicURL

        fetchFeedSched()

        binding.apply {
            edtUpdateName.setText(args.name)
            edtUpdateBreed.setText(args.breed)
            edtUpdateVaccinationDates.setText(args.vaccinationDates)
            amUpdateCB.isChecked = feedSched.morning
            noonUpdateCB.isChecked = feedSched.noon
            pmUpdateCB.isChecked =feedSched.night
            edtUpdateFoodAmount.setText(feedSched.amount)
            Picasso.get().load(imageUrl).into(imgUpdate)

            // Setup Spinner and DatePicker
            setupPetTypeSpinner(args.type)
            setupDatePicker()

            btnUpdate.setOnClickListener {
                updateData(userId!!)
                findNavController().navigate(R.id.action_updateFragment_to_homeFragment)
            }

            imgUpdate.setOnClickListener {
                pickImage.launch("image/*")
            }
        }

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

    private fun fetchFeedSched(){
        firestore.collection("feedSched")
            .whereEqualTo("petID", args.id)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Assuming there's only one document matching the query
                    feedSched = querySnapshot.documents.first().toObject(FeedSchedule::class.java)!!
                    Log.d("Firestore", "Feed Schedule: $feedSched")
                } else {
                    Log.d("Firestore", "No matching feed schedule found for petID: ${args.id}")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching feed schedule: ${exception.message}", exception)
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
            storageRef.child(petStoragePath).putFile(it)
                .addOnSuccessListener { task ->
                    task.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { imageUrl ->
                            val pet: MutableMap<String, Any> = hashMapOf(
                                "ownerID" to userId,
                                "type" to petType.toString(),
                                "breed" to breed,
                                "name" to name,
                                "vaccinationDates" to vaccinationDates,
                                "petPicURL" to imageUrl.toString()
                            )

                            // Save to Firestore
                            firestore.collection("pets").document(petId).set(pet)
                                .addOnCompleteListener {
                                    val feedSched: MutableMap<String, Any> = hashMapOf(
                                        "petID" to petId,
                                        "morning" to foodAM,
                                        "noon" to foodNoon,
                                        "night" to foodPM,
                                        "amount" to foodAmount
                                    )
                                    firestore.collection("feedSched")
                                        .document(this.feedSched.id!!).set(feedSched)
                                }
                        }
                }
        } ?: run {
            val pet: MutableMap<String, Any> = hashMapOf(
                "ownerID" to userId,
                "type" to petType.toString(),
                "breed" to breed,
                "name" to name,
                "vaccinationDates" to vaccinationDates,
                "petPicURL" to imageUrl.toString()
            )

            // Save to Firestore
            firestore.collection("pets").document(petId).set(pet)
                .addOnCompleteListener {
                    val feedSched: MutableMap<String, Any> = hashMapOf(
                        "petID" to petId,
                        "morning" to foodAM,
                        "noon" to foodNoon,
                        "night" to foodPM,
                        "amount" to foodAmount
                    )
                    firestore.collection("feedSched")
                        .document(this.feedSched.id!!).set(feedSched)
                }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
