package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.HomeFragmentDirections
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.RvPetsItemBinding
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.FeedSchedule
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.Pets

class RvPetsAdapter(private val petList: ArrayList<Pets>) : RecyclerView.Adapter<RvPetsAdapter.ViewHolder>() {

    class ViewHolder(val binding: RvPetsItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RvPetsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int {
        return petList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = petList[position]

        holder.apply {
            binding.apply {
                // Populate the TextViews with pet details
                tvNameItem.text = currentItem.name ?: "Unknown"
                tvBreedItem.text = currentItem.breed ?: "N/A"
                tvTypeItem.text = currentItem.type ?: "N/A"
                tvVaccinationDatesItem.text = currentItem.vaccinationDates ?: "N/A"


                // Load the pet's image using Picasso
                Picasso.get().load(currentItem.petPicURL).into(imgItem)

                // Set up the click listener to navigate to UpdateFragment
                rvContainer.setOnClickListener {
                    val action = HomeFragmentDirections.actionHomeFragmentToUpdateFragment(
                        currentItem.id ?: "",
                        currentItem.name ?: "",
                        currentItem.type ?: "",
                        currentItem.breed ?: "",
                        currentItem.vaccinationDates ?: "",
                        currentItem.petPicURL ?: ""
                    )
                    findNavController(holder.itemView).navigate(action)
                }

                // Set up a long-click listener to delete the pet
                rvContainer.setOnLongClickListener {
                    val context = holder.itemView.context
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Delete Pet")
                        .setMessage("Are you sure you want to delete this pet?")
                        .setPositiveButton("Delete") { _, _ ->
                            currentItem.id?.let { petId ->
                                deletePet(petId, currentItem.petPicURL, context)
                                petList.removeAt(position)
                                notifyItemRemoved(position)
                                Toast.makeText(context, "Pet deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
        }
    }

    private fun deletePet(petId: String, petPicURL: String?, context: Context) {
        val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val userId = firebaseAuth.currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val firebaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/pets")
        val firestoreRef = FirebaseFirestore.getInstance()

        // Delete from Realtime Database
        firebaseRef.child(petId).removeValue()
            .addOnCompleteListener {
                Toast.makeText(context, "Deleted from Realtime Database", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Failed to delete from Realtime Database: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        // Delete from Firestore
        firestoreRef.collection("users").document(userId)
            .collection("pets").document(petId).delete()
            .addOnCompleteListener {
                Toast.makeText(context, "Deleted from Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Failed to delete from Firestore: ${error.message}", Toast.LENGTH_SHORT).show()
            }

    }

}
