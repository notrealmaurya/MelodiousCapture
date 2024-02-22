package com.example.dtxvoicerecorder.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.dtxvoicerecorder.R
import com.example.dtxvoicerecorder.databinding.FragmentSplashBinding


class SplashFragment : Fragment() {

    private lateinit var fragmentSplashBinding: FragmentSplashBinding
    private lateinit var navController: NavController


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentSplashBinding = FragmentSplashBinding.inflate(inflater, container, false)
        return fragmentSplashBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)


        Handler(Looper.myLooper()!!).postDelayed(
            {
                navController.navigate(R.id.action_splashFragment_to_homeFragment)

            }, 2250
        )
    }
}