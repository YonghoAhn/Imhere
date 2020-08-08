package moe.misakachan.imhere

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_permission.*

private const val multiplePermissionsCode = 100;

//필요한 퍼미션 리스트
//원하는 퍼미션을 이곳에 추가하면 된다.
@RequiresApi(Build.VERSION_CODES.M)
private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NOTIFICATION_POLICY,
        Manifest.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND
    )
} else {
    arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NOTIFICATION_POLICY
    )
}


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
        button.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) checkPermissions()
            else
            {
                val extras = FragmentNavigatorExtras(
                    textView to "title",
                    textView2 to "subtitle",
                    button to "button"
                )

                findNavController().navigate(
                    R.id.action_permissionFragment_to_setAgeFragment,
                    null,
                    null,
                    extras
                )
            }
        }
    }

    private fun checkPermissions() : Boolean {
        //거절되었거나 아직 수락하지 않은 권한(퍼미션)을 저장할 문자열 배열 리스트
        val rejectedPermissionList = ArrayList<String>()

        //필요한 퍼미션들을 하나씩 끄집어내서 현재 권한을 받았는지 체크
        for (permission in requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //권한이 없다-> rejectedPermissionList
                rejectedPermissionList.add(permission)
            }
        }
        //거절된 퍼미션이 있다면
        return if (rejectedPermissionList.isNotEmpty()) {
            getPermission(rejectedPermissionList)
            true
        } else {
            val extras = FragmentNavigatorExtras(
                textView to "title",
                textView2 to "subtitle",
                button to "button"
            )

            findNavController().navigate(
                R.id.action_permissionFragment_to_setAgeFragment,
                null,
                null,
                extras
            )

            false
        }
            //권한 요청
    }

    private fun getPermission(rejectedPermissionList: ArrayList<String>)
    {
        val array = arrayOfNulls<String>(rejectedPermissionList.size)
        requestPermissions(rejectedPermissionList.toArray(array), multiplePermissionsCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            multiplePermissionsCode -> {
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            //권한 획득 실패
                            Toast.makeText(
                                requireContext(),
                                "Permission denied",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
