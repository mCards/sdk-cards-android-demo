# mCards Android Cards SDK Demo App

The mCards android Cards SDK encapsulates the following functionality:

Digital Provisioning Operations
1. Check whether or not Google Pay is installed
2. Install Google Pay
3. Sync the currently installed wallet
4. Check whether a card is in the digital wallet
5. Add a card to the digital wallet
6. Remove a card from the digital wallet
7. Activate a card that was partially added to the digital wallet

mCard Operations
1. Fetch a user's mCard list
2. Fetch a single mCard by ID
3. Fetch the different cash balances for a given mCard
4. Display mCard payment details (PAN, expiry, etc) via a secure webview

This demo app shows how to use the Auth SDK as a token provider in concert with the Cards SDK. It provides example code for a subset of the above SDK features, including digital provisioning.

# Integration
Implementing apps MUST override this string value for auth0 to work:

```<string name="auth0_domain">your value here</string>```

These values are gotten from the mCards team after setting up the client's auth0 instance.

You must then also update the manifest placeholders in the build.gradle file:

e.g. ```addManifestPlaceholders(mapOf("auth0Domain" to "@string/auth0_domain", "auth0Scheme" to "your app ID"))```

Use the customer package versions and build requirements listed below.

# Importing the Cards SDK
The mCards android SDKs are provided via a [bill of materials](https://github.com/mCards/sdk-bom-android). Add the following to your module-level build.gradle:

Groovy:
```
implementation(platform("com.mcards.sdk:bom:1.0.19"))
implementation "com.mcards.sdk:cards"
//implementation "com.mcards.sdk:auth" //only if also using the auth sdk as a token provider
```

Kotlin:
```
implementation(platform("com.mcards.sdk:bom:1.0.19"))
implementation("com.mcards.sdk:cards")
//implementation("com.mcards.sdk:auth") //only if also using the auth sdk as a token provider
```

And the following to the project settings.gradle (groovy):
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://maven.pkg.github.com/mcards/sdk-bom-android")
            credentials {
                username = GITHUB_USERNAME
                password = GITHUB_TOKEN
            }
        }
    }
}
```

## Google Pay Button

The Cards SDK provides the assets & styling to add correctly branded "Add to Google Pay" buttons to any app. Just add the following to any layout:

        <com.mcards.sdk.cards.view.GooglePayButton
            style="@style/GooglePayButton"/>

With additional styling and attributes as needed. Add a click listener like so (Java):

```
binding.[button ID].findViewById(com.mcards.sdk.cards.R.id.google_pay_btn_background).setOnClickListener();
```

# PIN management demo

Use customer BoM **1.0.19** (Cards **1.2.0**, Core **1.3.9.8**, Auth **1.2.8.6**).
The dependency versions and full release procedure are in the
[customer BoM repository](https://github.com/mCards/sdk-bom-android).
**Merge this demo update only after that release has been published.**

Build requirements: JDK 17+, Android compile SDK 37, minimum Android 11/API 30,
AGP 9.2.0, Gradle 9.5.0 and Kotlin 2.4.10. The Kotlin plugin is declared at the root
(without applying it to the app) to select the compiler used by AGP's built-in Kotlin.
AuthSDK 1.2.8.6 carries Kotlin 2.4 metadata, so the older default compiler cannot consume it.

Configure your assigned Auth0 domain/client/audience in the string resources and
`MainActivity`; the sample values are not a ready-to-use Tasted UAT configuration.
Log in with an assigned fictional UAT identity and an active Marqeta card. No shared
SMS inbox or test identity is provided in this repository.

Choose **Set / change PIN**, select a card, and complete the hosted flow. The demo refreshes
tokens before loading, enables JavaScript/cookies/DOM storage, and calls
`showSetPin(webView, card, idToken, listener)`. The SDK installs the bridge. Only
`PinAction.SET_PIN` success closes the dialog; activation/reveal events are not treated
as PIN completion. The SDK re-fetches the card and the demo retains that result, but
issuer confirmation may still be pending. Component errors allow retry, page load errors
are handled separately, and **Close** remains available. The WebView is destroyed on close.

The same flow sets or changes a PIN. Neither the app nor the listener handles the PIN
value. There are no theme or `success_url` arguments; component styling belongs to the
hosted page.

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

# Documentation
[Documentation site](https://mcards.readme.io/)

[SDKs conceptual documentation](https://mcards.readme.io/docs/mcards-sdk-overview)
