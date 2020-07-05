package moe.misakachan.imhere

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import kotlinx.android.synthetic.main.fragment_permission.*

class PermissionFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_permission, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val extras = FragmentNavigatorExtras(
            textView to "title",
            textView2 to "subtitle",
            button to "button"
        )
        //postponeEnterTransition()

        button.setOnClickListener {
            findNavController().navigate(R.id.action_permissionFragment_to_setAgeFragment,
                null,
                null,
                extras)
        }
    }

    private fun setSharedElementTransitionOnEnter() {

    }
}