package com.example.taptopayandroid.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.taptopayandroid.NavigationListener
import com.example.taptopayandroid.R

class PaymentDetails : Fragment() {
    companion object {
        const val TAG = "com.example.taptopayandroid.fragments.PaymentDetails"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_payment_details, container, false)

        var btnCollectPayment = view?.findViewById(R.id.collect_payment_button) as Button
        var cancelBtn = view?.findViewById(R.id.cancel_button) as Button
        var priceInput = view?.findViewById(R.id.price_input) as EditText

        btnCollectPayment.setOnClickListener {
            var amount = priceInput.text.toString()
            (activity as? NavigationListener)?.onCollectPayment(amount.toLong(), "usd",
                skipTipping = true,
                extendedAuth = false,
                incrementalAuth = false
            )
        }

        cancelBtn.setOnClickListener{
            (activity as? NavigationListener)?.onCancel()
        }

        return view
    }
}