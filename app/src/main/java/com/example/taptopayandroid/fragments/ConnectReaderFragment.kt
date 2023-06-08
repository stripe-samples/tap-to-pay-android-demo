package com.example.taptopayandroid.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.taptopayandroid.NavigationListener
import com.example.taptopayandroid.R

var btnConnectReader: Button? = null
var editPaymentDetailsButton: Button? = null
var currentReaderDetails: String? = null

class ConnectReaderFragment : Fragment() {
    companion object {
        const val TAG = "com.example.taptopayandroid.fragments.ConnectReaderFragment"
    }

//    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_connect_reader, container, false)

        btnConnectReader = view?.findViewById(R.id.connect_reader_button) as Button
        editPaymentDetailsButton = view?.findViewById(R.id.edit_payment_details_button) as Button

        // If the user is getting to this view after having already connected a reader
        if(currentReaderDetails !== null){
            val readerId = view?.findViewById(R.id.reader_id) as TextView
            readerId.text = currentReaderDetails

            btnConnectReader?.visibility = View.INVISIBLE
            editPaymentDetailsButton?.visibility = View.VISIBLE
        }

        btnConnectReader!!.setOnClickListener {
            btnConnectReader!!.text = "Connecting..."
            (activity as? NavigationListener)?.onConnectReader()
        }

        editPaymentDetailsButton!!.setOnClickListener{
            (activity as? NavigationListener)?.onNavigateToPaymentDetails()
        }

        return view
    }

    fun updateReaderId(location: String, reader_id: String){
        val readerId = view?.findViewById(R.id.reader_id) as TextView
        readerId.text = "$location : $reader_id"

        btnConnectReader?.visibility = View.INVISIBLE
        editPaymentDetailsButton?.visibility = View.VISIBLE

        currentReaderDetails = readerId.text as String
    }
}