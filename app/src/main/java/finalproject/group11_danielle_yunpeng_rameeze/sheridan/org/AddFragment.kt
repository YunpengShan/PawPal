package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentAddBinding
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.PetType
import java.util.Calendar

class AddFragment : Fragment() {
    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storageRef: StorageReference
    private lateinit var newFSID: String

    private var uri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        val userId = firebaseAuth.currentUser?.uid
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("users/$userId/pets")

        setupPetTypeSpinner()
        setupDatePicker() // Call the function to set up the date picker

        binding.btnSave.setOnClickListener {
            if (userId != null) {
                saveData(userId)
                findNavController().navigate(R.id.action_addFragment_to_homeFragment)
            } else {
                showAlertDialog("Error", "User not logged in!")
            }
        }

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
            binding.imgAdd.setImageURI(it)
            uri = it
        }

        binding.imgAdd.setOnClickListener {
            pickImage.launch("image/*")
        }

        return binding.root
    }

    private fun setupPetTypeSpinner() {
        val petTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            PetType.values().map { it.displayName }
        )
        petTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPetType.adapter = petTypeAdapter

        binding.spinnerPetType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedPetType = PetType.values()[position] // Get the selected pet type
                val drawableResId = getDrawableForPetType(selectedPetType) // Get corresponding drawable ID
                binding.typeImageView.setImageResource(drawableResId) // Set the ImageView resource
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optionally handle the case when no item is selected
            }
        }
    }

    private fun getDrawableForPetType(petType: PetType): Int {
        return when (petType) {
            PetType.DOG -> R.drawable.pp_dog
            PetType.CAT -> R.drawable.pp_cat
            PetType.BIRD -> R.drawable.pp_bird
            PetType.FISH -> R.drawable.pp_fish
            PetType.REPTILE -> R.drawable.pp_reptile
            else -> R.drawable.pets // Fallback image
        }
    }

    private fun setupDatePicker() {
        binding.edtVaccinationDates.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.edtVaccinationDates.setText(formattedDate) // Set selected date in the EditText
            }, year, month, day)

            datePickerDialog.show()
        }
    }

    private fun saveData(userId: String) {
        val name = binding.edtName.text.toString()
        val typeDisplayName = binding.spinnerPetType.selectedItem.toString() // Get selected type
        val breed = binding.edtBreed.text.toString()
        val vaccinationDates = binding.edtVaccinationDates.text.toString()

        val foodAM = binding.amCB.isChecked
        val foodNoon = binding.noonCB.isChecked
        val foodPM = binding.pmCB.isChecked
        val foodAmount = binding.edtFoodAmount.text.toString()

        if (name.isEmpty()) {
            binding.edtName.error = "Enter pet name"
            return
        }

        val petType = PetType.values().find { it.displayName == typeDisplayName } ?: return

        val petId = firebaseDatabase.push().key!!
        val petStoragePath = "petImages/$petId.jpg"

        uri?.let { imageUri ->
            storageRef.child(petStoragePath).putFile(imageUri)
                .addOnSuccessListener { task ->
                    task.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { imageUrl ->
                            val feedSched: MutableMap<String, Any> = hashMapOf(
                                "morning" to foodAM,
                                "noon" to foodNoon,
                                "night" to foodPM,
                                "amount" to foodAmount
                            )
                            firestore.collection("feedSched").add(feedSched)
                                .addOnSuccessListener { fsRef ->
                                    newFSID = fsRef.id

                                    val pet: MutableMap<String, Any> = hashMapOf(
                                        "ownerID" to userId,
                                        "type" to petType.toString(),
                                        "breed" to breed,
                                        "name" to name,
                                        "vaccinationDates" to vaccinationDates,
                                        "feedSchedID" to newFSID,
                                        "petPicURL" to imageUrl.toString()
                                    )

                                    firestore.collection("pets").document(petId).set(pet)
                                        .addOnCompleteListener {
                                            firestore.collection("users").document(userId)
                                                .update("NumPets", FieldValue.increment(1))
                                        }
                                }
                        }
                }
        } ?: run {
            showAlertDialog("Error", "Please select an image")
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", binding.edtName.text.toString())
        outState.putString("breed", binding.edtBreed.text.toString())
        outState.putString("vaccinationDates", binding.edtVaccinationDates.text.toString())
        outState.putString("foodAmount", binding.edtFoodAmount.text.toString())
        outState.putBoolean("foodAM", binding.amCB.isChecked)
        outState.putBoolean("foodNoon", binding.noonCB.isChecked)
        outState.putBoolean("foodPM", binding.pmCB.isChecked)
        outState.putInt("petTypePosition", binding.spinnerPetType.selectedItemPosition)
        outState.putParcelable("imageUri", uri)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            binding.edtName.setText(it.getString("name"))
            binding.edtBreed.setText(it.getString("breed"))
            binding.edtVaccinationDates.setText(it.getString("vaccinationDates"))
            binding.edtFoodAmount.setText(it.getString("foodAmount"))
            binding.amCB.isChecked = it.getBoolean("foodAM")
            binding.noonCB.isChecked = it.getBoolean("foodNoon")
            binding.pmCB.isChecked = it.getBoolean("foodPM")
            binding.spinnerPetType.setSelection(it.getInt("petTypePosition"))
            uri = it.getParcelable("imageUri")
            uri?.let { imageUri ->
                binding.imgAdd.setImageURI(imageUri)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
