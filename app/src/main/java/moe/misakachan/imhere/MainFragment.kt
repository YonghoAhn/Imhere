package moe.misakachan.imhere

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    private val mService = WalkTrackerService()
    private val mIntent by lazy { Intent(requireActivity(),mService.javaClass) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        button.setOnClickListener {
            if(!Util.isMyServiceRunning(mService.javaClass,requireActivity()))
            {
                //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                //    requireActivity().startForegroundService(mIntent)
                //} else {
                    requireActivity().startService(mIntent)
                //}
                Toast.makeText(requireContext(), "서비스가 시작되었습니다.", Toast.LENGTH_SHORT).show()
                textView.text = "실행중"
                textView.setTextColor(resources.getColor(R.color.running) )
                cardView.strokeColor = resources.getColor(R.color.running)
                button.text = "Tap to Off"
            }
            else
            {
                requireActivity().stopService(mIntent)
                Toast.makeText(requireContext(), "서비스가 종료되었습니다.", Toast.LENGTH_SHORT).show()
                textView.text = "종료됨"
                textView.setTextColor(resources.getColor(R.color.notrunning ) )
                cardView.strokeColor = resources.getColor(R.color.notrunning)
                button.text = "Tap to on"
            }
        }
    }

}