package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.HomeFragmentDirections
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.R
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.RvPetsItemBinding
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.FeedSchedule
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.PetType
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
                tvTypeItem.setImageResource(getDrawableForPetType(currentItem.type!!))
                tvVaccinationDatesItem.text = currentItem.vaccinationDates ?: "N/A"

                // Load the pet's image using Picasso
                Picasso.get().load(currentItem.petPicURL).into(imgItem)

                // Fetch the feed schedule using currentItem.feedSchedID
                currentItem.feedSchedID?.let { feedSchedID ->
                    fetchFeedSchedule(feedSchedID,
                        onSuccess = { feedSchedule ->
                            // Update the UI with the fetched feed schedule
                            Log.d("FeedSchedule", "Fetched feed schedule: $feedSchedule")
                            amIV.visibility = if (feedSchedule.morning) View.VISIBLE else View.INVISIBLE
                            noonIV.visibility = if (feedSchedule.noon) View.VISIBLE else View.INVISIBLE
                            pmIV.visibility = if (feedSchedule.night) View.VISIBLE else View.INVISIBLE
                            tvFeedAmt.text = feedSchedule.amount
                        },
                        onFailure = { exception ->
                            Log.e("FeedSchedule", "Error fetching feed schedule: ${exception.message}")
                        }
                    )
                }

                // Set up the click listener to navigate to UpdateFragment
                rvContainer.setOnClickListener {
                    val action = HomeFragmentDirections.actionHomeFragmentToUpdateFragment(
                        currentItem.id ?: "",
                        currentItem.name ?: "",
                        currentItem.type ?: "",
                        currentItem.breed ?: "",
                        currentItem.vaccinationDates ?: "",
                        currentItem.feedSchedID ?: "",
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
                                deletePet(petId, currentItem.petPicURL, currentItem.feedSchedID, context)
                                petList.removeAt(position)
                                notifyItemRemoved(position)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
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
            // Add other pet types and corresponding drawables
            else -> R.drawable.pets // Fallback image
        }
    }

    private fun fetchFeedSchedule(feedSchedID: String, onSuccess: (FeedSchedule) -> Unit, onFailure: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("feedSched").document(feedSchedID)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val feedSchedule = documentSnapshot.toObject(FeedSchedule::class.java)
                    if (feedSchedule != null) {
                        onSuccess(feedSchedule) // Pass the feedSchedule to the onSuccess callback
                    } else {
                        onFailure(Exception("FeedSchedule document is null"))
                    }
                } else {
                    onFailure(Exception("No FeedSchedule document found for ID: $feedSchedID"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception) // Pass the exception to the onFailure callback
            }
    }

    private fun deletePet(petId: String, petPicURL: String?, fsID: String?, context: Context) {
        val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val userId = firebaseAuth.currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val firestoreRef = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance().reference

        // Delete from Firestore
        //Delete Feeding Schedule
        if (fsID != null) {
            firestoreRef.collection("feedSched").document(fsID).delete()
                .addOnSuccessListener {
                    //Decrement User Num Pets
                    firestoreRef.collection("users").document(userId)
                        .update("NumPets", FieldValue.increment(-1))
                        .addOnCompleteListener {
                            //Delete Pet Image from Storage
                            storage.child("petImages/$petId.jpg").delete()
                                .addOnSuccessListener {
                                    //Delete pet
                                    firestoreRef.collection("pets").document(petId).delete()
                                        .addOnCompleteListener {
                                            Toast.makeText(context, "Deleted from Firestore", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { error ->
                                            Toast.makeText(context, "Failed to delete from Firestore: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }

                        }

                }
        }


    }

}
