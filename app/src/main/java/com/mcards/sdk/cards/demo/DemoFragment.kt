package com.mcards.sdk.cards.demo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.mcards.sdk.auth.AuthSdk
import com.mcards.sdk.auth.AuthSdkProvider
import com.mcards.sdk.auth.model.auth.User
import com.mcards.sdk.cards.CardsSdk
import com.mcards.sdk.cards.CardsSdkProvider
import com.mcards.sdk.cards.demo.databinding.FragmentDemoBinding
import com.mcards.sdk.cards.model.CardStatus
import com.mcards.sdk.cards.model.PinAction
import com.mcards.sdk.cards.model.WalletResponse
import com.mcards.sdk.cards.model.WalletStatus
import com.mcards.sdk.core.InvalidTokenCallback
import com.mcards.sdk.core.model.AuthTokens
import com.mcards.sdk.core.model.card.Card
import com.mcards.sdk.core.network.model.SdkResult
import com.mcards.sdk.core.util.LoggingCallback
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

private const val TEST_PHONE_NUMBER = "" // Use an assigned UAT identity, not a shared SMS inbox.

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DemoFragment : Fragment() {

    private var _binding: FragmentDemoBinding? = null
    private val binding get() = _binding!!
    private val cardsSdk = CardsSdkProvider.getInstance()

    private var userPhoneNumber = ""
    private var accessToken = ""
    private var idToken = ""
    private var loggedIn = false

    private var card: Card? = null
    private var cards: List<Card> = emptyList()
    private val subscriptions = CompositeDisposable()
    private var pinDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDemoBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addToWallet.setOnClickListener {
            if (loggedIn && card != null) {
                addToWallet()
            } else {
                Snackbar.make(view, "Login first", BaseTransientBottomBar.LENGTH_LONG).show()
            }
        }

        binding.setPin.setOnClickListener { selectPinCard() }

        val loginCallback = object : AuthSdk.Auth0Callback {
            override fun onSuccess(
                user: User,
                tokens: AuthTokens,
                regionChanged: Boolean,
                cardId: String?
            ) {
                accessToken = tokens.accessToken
                idToken = tokens.idToken
                userPhoneNumber = user.userClaim.phoneNumber
                loggedIn = true
                card = null
                cards = emptyList()

                activity?.runOnUiThread {
                    initCardsSdk()
                }
            }

            override fun onFailure(message: String) {
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
            }
        }

        val authSdk = AuthSdkProvider.getInstance()
        binding.loginBtn.setOnClickListener {
            if (userPhoneNumber.isBlank()) {
                authSdk.auth0Authenticate(requireContext(), TEST_PHONE_NUMBER, loginCallback)
            } else {
                authSdk.auth0Authenticate(requireContext(), userPhoneNumber, loginCallback)
            }
        }
    }

    @SuppressLint("CheckResult")
    @MainThread
    private fun initCardsSdk() {
        cardsSdk.init(requireActivity(),
            accessToken,
            debug = BuildConfig.DEBUG,
            object : InvalidTokenCallback {
                override fun onTokenInvalid(): String {
                    val tokens = AuthSdkProvider.getInstance().refreshAuth0Tokens()
                    accessToken = tokens.accessToken
                    idToken = tokens.idToken
                    return tokens.accessToken
                }
            })

        //optional, to use your standard logging methods. Needs to be set on each sdk individually
        cardsSdk.setLoggingCallback(object : LoggingCallback {
            override fun log(t: Throwable) {
                //TODO log exception
            }

            override fun log(msg: String) {
                //TODO log message
            }
        })

        cardsSdk.setWalletCallback(object : CardsSdk.WalletCallback {
            override fun onDataChanged(liveData: LiveData<WalletResponse<WalletStatus>>) {
                observeSync(liveData)
            }
        })

        observeSync(cardsSdk.syncWallet())

        cardsSdk.getCardsList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : SingleObserver<SdkResult<List<Card>>> {
                override fun onSubscribe(d: Disposable) {
                    subscriptions.add(d)
                    activity?.runOnUiThread {
                        binding.progressbar.visibility = View.VISIBLE
                    }
                }

                override fun onError(e: Throwable) {
                    if (_binding == null) return
                    activity?.runOnUiThread {
                        binding.progressbar.visibility = View.GONE
                        Snackbar.make(
                            requireView(),
                            e.localizedMessage!!,
                            BaseTransientBottomBar.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onSuccess(t: SdkResult<List<Card>>) {
                    if (_binding == null) return
                    binding.progressbar.visibility = View.GONE
                    cards = t.result.orEmpty()
                    card = cards.firstOrNull()
                    t.result?.let {
                        Snackbar.make(requireView(), "Fetched cards successfully, debug to inspect data", BaseTransientBottomBar.LENGTH_LONG)
                            .show()
                    } ?: t.errorMsg?.let {
                        activity?.runOnUiThread {
                            Snackbar.make(requireView(), it, BaseTransientBottomBar.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            })
    }

    override fun onStart() {
        super.onStart()
        if (loggedIn) {
            observeSync(cardsSdk.syncWallet())
        }
    }

    private fun observeSync(liveData: LiveData<WalletResponse<WalletStatus>>) {
        requireActivity().runOnUiThread {
            liveData.observe(viewLifecycleOwner) {
                updateWalletStatus(it)
            }
        }
    }

    private fun updateWalletStatus(response: WalletResponse<WalletStatus>) {
        val status: WalletResponse.Status = response.status
        if (status == WalletResponse.Status.BUSY) {
            binding.progressbar.visibility = View.VISIBLE
            // TODO normally we want to disable the add to wallet button if we know the operation
            //  can't succeed, but for demo purposes we're leaving it enabled.
            //binding.addToWallet.isEnabled = false
        } else {
            binding.progressbar.visibility = View.GONE
            if (status == WalletResponse.Status.FAILURE) {
                val e: Exception? = response.exception
                var msg = "Failed to sync wallet"
                if (e != null) {
                    msg = e.toString()
                }

                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            } else if (status == WalletResponse.Status.SUCCESS) {
                val walletStatus = response.data
                if (walletStatus != null) {
                    Toast.makeText(requireContext(), walletStatus.toString(), Toast.LENGTH_LONG).show()

                    if (walletStatus == WalletStatus.WALLET_AVAILABLE) {
                        getCardStatus()
                    }
                }
            }
        }
    }

    private fun updateCardStatus(response: WalletResponse<CardStatus>) {
        val status = response.status
        if (status == WalletResponse.Status.BUSY) {
            binding.progressbar.visibility = View.VISIBLE
            binding.addToWallet.isEnabled = false
        } else {
            binding.addToWallet.visibility = View.VISIBLE
            binding.progressbar.visibility = View.GONE
            if (status == WalletResponse.Status.FAILURE) {
                Toast.makeText(requireContext(), response.exception!!.message, Toast.LENGTH_LONG).show()
            } else if (status == WalletResponse.Status.SUCCESS) {
                val data = response.data
                Toast.makeText(requireContext(), "Card status: $data", Toast.LENGTH_LONG).show()

                when (data) {
                    CardStatus.NOT_ADDED -> {
                        binding.addToWallet.isEnabled = true
                    }
                    CardStatus.CARD_UNKNOWN -> {
                        getCardStatus()
                    }
                    CardStatus.ADDED -> {
                        binding.addToWallet.visibility = View.GONE
                    }
                    CardStatus.PENDING -> {
                        //TODO take appropriate action
                    }
                    CardStatus.USER_UNVERIFIED -> {
                        // in this case we are auto-activating the card, but you could prompt the
                        // user first
                        activateCard()
                    }
                    CardStatus.SUSPENDED -> {
                        //TODO take appropriate action
                    }
                    null -> {}
                }
            }
        }
    }

    private fun getCardStatus() {
        requireActivity().runOnUiThread {
            val selectedCard = card ?: return@runOnUiThread
            cardsSdk.getCardWalletStatus(selectedCard).observe(viewLifecycleOwner) {
                updateCardStatus(it)
            }
        }
    }

    private fun addToWallet() {
        requireActivity().runOnUiThread {
            val selectedCard = card ?: return@runOnUiThread
            // result of this is caught in onActivityResult() but can be safely ignored
            cardsSdk.addCardToWallet(selectedCard).observe(viewLifecycleOwner) {
                updateCardStatus(it)
            }
        }
    }

    private fun activateCard() {
        requireActivity().runOnUiThread {
            val selectedCard = card ?: return@runOnUiThread
            // result of this is caught in onActivityResult() but can be safely ignored
            cardsSdk.activateCardInWallet(selectedCard).observe(viewLifecycleOwner) {
                updateCardStatus(it)
            }
        }
    }

    private fun selectPinCard() {
        val eligible = cards.filter { it.issuer == Card.Issuer.MARQETA && it.status == Card.Status.ACTIVE }
        if (eligible.isEmpty()) {
            Snackbar.make(binding.root, "Log in with an active Marqeta UAT card first", Snackbar.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Set / change PIN")
            .setItems(eligible.map { "${it.details?.title.orEmpty()} •••• ${it.details?.last4Digits.orEmpty()}" }.toTypedArray()) { _, index ->
                refreshTokensAndOpenPin(eligible[index])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshTokensAndOpenPin(selectedCard: Card) {
        // WebView loads don't go through the SDK API token-refresh interceptor.
        subscriptions.add(Single.fromCallable { AuthSdkProvider.getInstance().refreshAuth0Tokens() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ tokens ->
                if (_binding == null) return@subscribe
                accessToken = tokens.accessToken
                idToken = tokens.idToken
                cardsSdk.init(requireActivity(), accessToken, debug = false, object : InvalidTokenCallback {
                    override fun onTokenInvalid(): String =
                        AuthSdkProvider.getInstance().refreshAuth0Tokens().accessToken
                })
                showPin(selectedCard)
            }, { error ->
                _binding?.let { Snackbar.make(it.root, error.message ?: "Login expired", Snackbar.LENGTH_LONG).show() }
            }))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showPin(selectedCard: Card) {
        pinDialog?.dismiss()
        val webView = WebView(requireContext()).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.7).toInt())
        }
        CookieManager.getInstance().setAcceptCookie(true)
        val content = LinearLayout(requireContext()).apply { addView(webView) }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Set / change PIN")
            .setView(content)
            .setNegativeButton("Close", null)
            .create()
        pinDialog = dialog
        fun showError(message: String) {
            if (dialog.isShowing) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showError("Could not load PIN page. Close and try again.")
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, error: WebResourceResponse) {
                if (request.isForMainFrame) showError("PIN page returned ${error.statusCode}. Close and try again.")
            }
        }
        dialog.setOnDismissListener {
            webView.stopLoading()
            webView.removeJavascriptInterface("MCardsPinBridge")
            content.removeView(webView)
            webView.destroy()
            if (pinDialog === dialog) pinDialog = null
        }
        dialog.show()
        cardsSdk.showSetPin(webView, selectedCard, idToken, object : CardsSdk.PinActionListener {
            override fun onPinActionCompleted(action: PinAction, card: Card?) {
                // Activation/reveal completion must not finish a set-PIN flow.
                if (action != PinAction.SET_PIN || !dialog.isShowing) return
                dialog.dismiss()
                // The SDK has already re-fetched this card; PIN confirmation can still be pending.
                card?.let { updated ->
                    cards = cards.map { if (it.uuid == updated.uuid) updated else it }
                    if (this@DemoFragment.card?.uuid == updated.uuid) this@DemoFragment.card = updated
                }
                _binding?.let { Snackbar.make(it.root, "PIN submitted; issuer confirmation may be pending", Snackbar.LENGTH_LONG).show() }
            }
            override fun onPinActionFailed(action: PinAction, error: String?) {
                showError(error ?: "PIN update failed. Retry in the page or close this screen.")
            }
        })
    }

    override fun onDestroyView() {
        pinDialog?.dismiss()
        subscriptions.clear()
        super.onDestroyView()
        _binding = null
    }
}
