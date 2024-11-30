package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        val userId = firebaseAuth.currentUser?.uid
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("users/$userId/pets")

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
            binding.imgUpdate.setImageURI(it)
            uri = it
        }
        imageUrl = args.petPicURL

        binding.apply {
            edtUpdateName.setText(args.name)
            edtUpdateBreed.setText(args.breed)
            edtUpdateVaccinationDates.setText(args.vaccinationDates)
            edtUpdateFeedingSchedule.setText(args.feedingSchedule)
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

        return binding.root
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

    private fun updateData(userId: String) {
        val name = binding.edtUpdateName.text.toString()
        val breed = binding.edtUpdateBreed.text.toString()
        val vaccinationDates = binding.edtUpdateVaccinationDates.text.toString()
        val feedingSchedule = binding.edtUpdateFeedingSchedule.text.toString()
        val petTypeDisplayName = binding.spinnerPetTypeUpdate.selectedItem.toString()

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
                            val updatedPet = Pets(
                                id = petId,
                                type = petType.toString(),
                                breed = breed,
                                name = name,
                                vaccinationDates = vaccinationDates,
                                feedingSchedule = feedingSchedule,
                                petPicURL = imageUrl.toString()
                            )
                            saveUpdatedPet(userId, updatedPet)
                        }
                }
        } ?: run {
            val updatedPet = Pets(
                id = petId,
                type = petType.toString(),
                breed = breed,
                name = name,
                vaccinationDates = vaccinationDates,
                feedingSchedule = feedingSchedule,
                petPicURL = imageUrl
            )
            saveUpdatedPet(userId, updatedPet)
        }
    }

    private fun saveUpdatedPet(userId: String, pet: Pets) {
        firebaseDatabase.child(pet.id.toString()).setValue(pet)
            .addOnCompleteListener {
                Toast.makeText(context, "Pet updated in Realtime Database!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        firestore.collection("users").document(userId)
            .collection("pets").document(pet.id.toString()).set(pet)
            .addOnCompleteListener {
                Toast.makeText(context, "Pet updated in Firestore!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
