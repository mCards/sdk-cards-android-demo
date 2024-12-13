# mCards Android Cards SDK Demo App

The mCards android Cards SDK encapsulates the following functionality:

1. Digital provisioning via Google Pay
2. Check whether or not Google Pay is installed
3. Install Google Pay
4. Sync the currently installed wallet
5. Check whether a card is in the digital wallet
6. Add a card to the digital wallet
7. Remove a card from the digital wallet
8. Continue adding a card that was partially added to the digital wallet
9. Fetch a user's mCard list
10. Fetch a single mCard by ID
11. Fetch the balances for a given mCard
12. Display mCard payment details via a secure webview

# Usage
Implementing apps MUST override this string value for auth0 to work:

<string name="auth0_domain">your value here</string>

These values are gotten from the mCards team after setting up the client's auth0 instance.

You must then also update the manifest placeholders in the build.gradle file:

e.g. addManifestPlaceholders(mapOf("auth0Domain" to "@string/auth0_domain", "auth0Scheme" to "your app ID"))


# Importing the Auth SDK
Add the following to your module-level build.gradle:

Groovy:
```
implementation "com.mcards.sdk:cards:$latestVersion"
```

Kotlin:
```
implementation("com.mcards.sdk:cards:$latestVersion")
```

And the following to the project settings.gradle:
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://maven.pkg.github.com/Wantsa/sdk-cards-android")
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
