# Tap to Pay with Stripe - Android demo

This is a small demo using Tap to Pay with Stripe for Android. The flow is kept very simple to focus on teaching developers how to integrate Tap to Pay with Stripe.

If you want to learn how to add Tap to Pay to an Android app, check out the [blog post](https://dev.to/stripe/accept-payments-using-tap-to-pay-for-android-with-stripe-23ml), [tutorial video](https://youtu.be/2y0abSgxPXw) and [our docs](https://stripe.com/docs/terminal/payments/setup-reader/tap-to-pay?platform=android) for more information.


## Installation

You can clone this repo and run it using Android Studio. Using the Stripe Terminal SDK requires a back-end server to run, and it needs to be available with a public URL.

In the `graddle.properties` file, you will find the variable `EXAMPLE_BACKEND_URL` where you need to indicate the URL to an app hosted and available publicly. If you want to get started quickly, you can clone [this example back-end](https://github.com/stripe/example-terminal-backend), update it to use your Stripe publishable and secret keys, host it on Render or any service that will give you a public URL, and replace the `EXAMPLE_BACKEND_URL` with it.

## Create a location

In the [Stripe dashboard](https://dashboard.stripe.com/terminal), you need to create at least one location to manage your reader.

Once this is done, you should be able to run the application successfully.

## Demo

Once you run the demo on your mobile device, you should see the following screens:

<img src="ttp-android-demo.gif"  width="60%" height="30%">

