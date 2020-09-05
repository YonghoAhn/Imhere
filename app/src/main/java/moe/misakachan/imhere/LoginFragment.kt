package moe.misakachan.imhere

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_set_age.*

class LoginFragment : Fragment() {

    private val mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(mAuth.currentUser != null)
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)

        button2.setOnClickListener {
            mAuth.signInAnonymously().addOnCompleteListener {
                if(it.isSuccessful)
                {
                    db.collection("ids").document("uuids").get().addOnSuccessListener { document ->
                        if(document != null)
                        {
                            val major = document.getLong("major")
                            val minor = document.getLong("minor")

                            if (major != null && minor != null) {
                                requireContext().getSharedPreferences("imhere", Context.MODE_PRIVATE).edit()
                                    .putLong("major", major).apply()
                                db.collection("ids").document("uuids").update("major",major+1)
                                requireContext().getSharedPreferences("imhere", Context.MODE_PRIVATE).edit()
                                    .putLong("minor", minor).apply()
                                db.collection("ids").document("uuids").update("minor", minor+1)
                                val docData = hashMapOf(
                                    "major" to major,
                                    "minor" to minor,
                                    "currentPosition" to GeoPoint(0.0,0.0),
                                    "beforePosition" to GeoPoint(0.0,0.0)
                                )
                                db.collection("ids").document("users").collection("user").document().set(docData).addOnSuccessListener {
                                    if(requireContext().getSharedPreferences("imhere", Context.MODE_PRIVATE).getString("setting","false") == "true")
                                        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                                    else
                                        findNavController().navigate(R.id.action_loginFragment_to_setAgeFragment)
                                }.addOnFailureListener { e->
                                    Log.d("MisakaMOE", "Error : ${e.message}")
                                    Toast.makeText(context,"일시적인 오류가 발생했습니다.",Toast.LENGTH_SHORT).show()
                                }
                            }

                        } else {
                            Toast.makeText(context,"UUID 증가 중 일시적인 오류가 발생했습니다.",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else
                {
                    Log.d("MisakaMOE","Error : ${it.exception?.message}")
                    Toast.makeText(context,"일시적인 오류가 발생해 가입에 실패했습니다.",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}