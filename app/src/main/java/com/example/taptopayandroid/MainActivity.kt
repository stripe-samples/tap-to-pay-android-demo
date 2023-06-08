package com.example.taptopayandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.taptopayandroid.fragments.ConnectReaderFragment
import com.example.taptopayandroid.fragments.PaymentDetails
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.*
import com.stripe.stripeterminal.external.models.*
import com.stripe.stripeterminal.log.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.Call
import retrofit2.Response

var SKIP_TIPPING: Boolean = true

class MainActivity : AppCompatActivity(), NavigationListener {
    // Register the permissions callback to handles the response to the system permissions dialog.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        ::onPermissionResult
    )

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigateTo(ConnectReaderFragment.TAG, ConnectReaderFragment(), false)

        requestPermissionsIfNecessarySdk31()

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            BluetoothAdapter.getDefaultAdapter()?.let { adapter ->
                if (!adapter.isEnabled) {
                    adapter.enable()
                }
            }
        } else {
            Log.w(MainActivity::class.java.simpleName, "Failed to acquire Bluetooth permission")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestPermissionsIfNecessarySdk31() {
        // Check for location and bluetooth permissions
        val deniedPermissions = mutableListOf<String>().apply {
            if (!isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (!isGranted(Manifest.permission.BLUETOOTH_CONNECT)) add(Manifest.permission.BLUETOOTH_CONNECT)
            if (!isGranted(Manifest.permission.BLUETOOTH_SCAN)) add(Manifest.permission.BLUETOOTH_SCAN)
        }.toTypedArray()

        if (deniedPermissions.isNotEmpty()) {
            // If we don't have them yet, request them before doing anything else
            requestPermissionLauncher.launch(deniedPermissions)
        } else if (!Terminal.isInitialized() && verifyGpsEnabled()) {
            initialize()
        }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun onPermissionResult(result: Map<String, Boolean>) {
        val deniedPermissions: List<String> = result
            .filter { !it.value }
            .map { it.key }

        // If we receive a response to our permission check, initialize
        if (deniedPermissions.isEmpty() && !Terminal.isInitialized() && verifyGpsEnabled()) {
            initialize()
        }
    }

    private fun verifyGpsEnabled(): Boolean {
        val locationManager: LocationManager? =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var gpsEnabled = false

        try {
            gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        } catch (exception: Exception) {}

        if (!gpsEnabled) {
            // notify user
        }

        return gpsEnabled
    }

    private fun initialize() {
        // Initialize the Terminal as soon as possible
        try {
            Terminal.initTerminal(
                applicationContext, LogLevel.VERBOSE, TokenProvider(),
                TerminalEventListener()
            )
        } catch (e: TerminalException) {
            throw RuntimeException(
                "Location services are required in order to initialize " +
                        "the Terminal.",
                e
            )
        }

        loadLocations()
    }

    private val mutableListState = MutableStateFlow(LocationListState())

    private val locationCallback = object : LocationListCallback {
        override fun onFailure(e: TerminalException) {
            e.printStackTrace()
        }

        override fun onSuccess(locations: List<Location>, hasMore: Boolean) {
            mutableListState.value = mutableListState.value.let {
                it.copy(
                    locations = it.locations + locations,
                    hasMore = hasMore,
                    isLoading = false,
                )
            }
        }
    }

    private fun collectPayment(
        amount: Long,
        currency: String,
        skipTipping: Boolean,
        extendedAuth: Boolean,
        incrementalAuth: Boolean
    ){
        SKIP_TIPPING = skipTipping

        ApiClient.createPaymentIntent(
            amount,
            currency,
            extendedAuth,
            incrementalAuth,
            callback = object : retrofit2.Callback<PaymentIntentCreationResponse> {
                override fun onResponse(
                    call: Call<PaymentIntentCreationResponse>,
                    response: Response<PaymentIntentCreationResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        Terminal.getInstance().retrievePaymentIntent(
                            response.body()?.secret!!,
                            createPaymentIntentCallback
                        )
                    } else {
                        println("Request not successful: ${response.body()}")
                    }
                }

                override fun onFailure(
                    call: Call<PaymentIntentCreationResponse>,
                    t: Throwable
                ) {
                    t.printStackTrace()
                }
            }
        )
    }

    private val createPaymentIntentCallback by lazy {
        object : PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {
                val skipTipping = SKIP_TIPPING

                val collectConfig = CollectConfiguration.Builder()
                    .skipTipping(skipTipping)
                    .build()

                Terminal.getInstance().collectPaymentMethod(
                    paymentIntent, collectPaymentMethodCallback, collectConfig
                )
            }

            override fun onFailure(e: TerminalException) {
                e.printStackTrace()
            }
        }
    }

    private val collectPaymentMethodCallback by lazy {
        object : PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {
                Terminal.getInstance().processPayment(paymentIntent, processPaymentCallback)
            }

            override fun onFailure(e: TerminalException) {
                e.printStackTrace()
            }
        }
    }

    private val processPaymentCallback by lazy {
        object : PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {
                ApiClient.capturePaymentIntent(paymentIntent.id)

                //TODO : Return to previous Screen
                navigateTo(PaymentDetails.TAG, PaymentDetails(), true)
            }

            override fun onFailure(e: TerminalException) {
                e.printStackTrace()
            }
        }
    }

    private fun loadLocations() {
        Terminal.getInstance().listLocations(
            ListLocationsParameters.Builder().apply {
                limit = 100
            }.build(),
            locationCallback
        )
    }

    private fun connectReader(){
        val config = DiscoveryConfiguration(
            timeout = 0,
            discoveryMethod = DiscoveryMethod.LOCAL_MOBILE,
            isSimulated = false,
            location = mutableListState.value.locations[0].id
        )

        Terminal.getInstance().discoverReaders(config, discoveryListener = object :
            DiscoveryListener {
            override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                readers.filter { it.networkStatus != Reader.NetworkStatus.OFFLINE }
                var reader = readers[0]

                val config = ConnectionConfiguration.LocalMobileConnectionConfiguration("${mutableListState.value.locations[0].id}")

                Terminal.getInstance().connectLocalMobileReader(
                    reader,
                    config,
                    object: ReaderCallback {
                        override fun onFailure(e: TerminalException) {
                            e.printStackTrace()
                        }

                        override fun onSuccess(reader: Reader) {
                            // Update the UI with the location name and terminal ID
                            runOnUiThread {
                                val manager: FragmentManager = supportFragmentManager
                                val fragment: Fragment? = manager.findFragmentByTag(ConnectReaderFragment.TAG)

                                if(reader.id !== null && mutableListState.value.locations[0].displayName !== null){
                                    (fragment as ConnectReaderFragment).updateReaderId(
                                        mutableListState.value.locations[0].displayName!!, reader.id!!
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }, object : Callback {
            override fun onSuccess() {
                println("Finished discovering readers")
            }

            override fun onFailure(e: TerminalException) {
                e.printStackTrace()
            }
        })
    }

    // Navigate to Fragment
    private fun navigateTo(
        tag: String,
        fragment: Fragment,
        replace: Boolean = true,
        addToBackStack: Boolean = false,
    ) {
        val frag = supportFragmentManager.findFragmentByTag(tag) ?: fragment
        supportFragmentManager
            .beginTransaction()
            .apply {
                if (replace) {
                    replace(R.id.container, frag, tag)
                } else {
                    add(R.id.container, frag, tag)
                }

                if (addToBackStack) {
                    addToBackStack(tag)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun onConnectReader(){
        connectReader()
    }

    override fun onCollectPayment(
        amount: Long,
        currency: String,
        skipTipping: Boolean,
        extendedAuth: Boolean,
        incrementalAuth: Boolean
    ){
        collectPayment(amount, currency, skipTipping, extendedAuth, incrementalAuth)
    }

    override fun onNavigateToPaymentDetails(){
        // Navigate to the fragment that will show the payment details
        navigateTo(PaymentDetails.TAG, PaymentDetails(), true)
    }

    override fun onCancel(){
        navigateTo(ConnectReaderFragment.TAG, ConnectReaderFragment(), true)
    }
}