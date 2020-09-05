package moe.misakachan.imhere

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import kotlinx.android.synthetic.main.fragment_set_age.*
import java.lang.Integer.parseInt


class SetAgeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_age, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        button.setOnClickListener {
            val age = parseInt(txtInput.editText?.text.toString())
            if (age in 4..120) {
                requireContext().getSharedPreferences("imhere", Context.MODE_PRIVATE).edit()
                    .putString(
                        "age",
                        txtInput.editText?.text.toString())
                    .putString(
                        "setting",
                        "true")
                    .apply()
                findNavController().navigate(R.id.action_setAgeFragment_to_mainFragment)
            }
            else{
                txtAge.error = R.string.error_age.toString();
            }
        }
    }


}