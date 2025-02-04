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

# Usage
Implementing apps MUST override this string value for auth0 to work:

"<string name="auth0_domain">your value here</string>"

These values are gotten from the mCards team after setting up the client's auth0 instance.

You must then also update the manifest placeholders in the build.gradle file:

e.g. addManifestPlaceholders(mapOf("auth0Domain" to "@string/auth0_domain", "auth0Scheme" to "your app ID"))


# Importing the Cards SDK
The mCards android SDKs are provided via a bill of materials. Add the following to your module-level build.gradle:

Groovy:
```
implementation(platform("com.mcards.sdk:bom:$latestVersion"))
implementation "com.mcards.sdk:cards"
//implementation "com.mcards.sdk:auth" //only if also using the auth sdk as a token provider
```

Kotlin:
```
implementation(platform("com.mcards.sdk:bom:$latestVersion"))
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
            url = uri("https://maven.pkg.github.com/Wantsa/sdk-bom-android")
            credentials {
                username = GITHUB_USERNAME
                password = GITHUB_TOKEN
            }
        }
    }
}
```

# Documentation
\\\\\Add documentation links here/////
