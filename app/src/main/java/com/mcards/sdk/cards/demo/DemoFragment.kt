package com.mcards.sdk.cards.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.mcards.sdk.auth.AuthSdk
import com.mcards.sdk.auth.AuthSdkProvider
import com.mcards.sdk.auth.model.auth.User
import com.mcards.sdk.cards.CardsSdk
import com.mcards.sdk.cards.CardsSdkProvider
import com.mcards.sdk.cards.CardsViewModel
import com.mcards.sdk.cards.demo.databinding.FragmentDemoBinding
import com.mcards.sdk.cards.model.CardStatus
import com.mcards.sdk.cards.model.WalletResponse
import com.mcards.sdk.cards.model.WalletStatus
import com.mcards.sdk.core.model.AuthTokens
import com.mcards.sdk.core.model.card.Card
import com.mcards.sdk.core.util.Views

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DemoFragment : Fragment() {

    private var _binding: FragmentDemoBinding? = null
    private val binding get() = _binding!!
    private val cardsVM: CardsViewModel by activityViewModels()
    private val cardsSdk = CardsSdkProvider.getInstance()

    private var userPhoneNumber = ""
    private var accessToken = ""
    private var idToken = ""
    private var loggedIn = false

    private lateinit var card: Card

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDemoBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addToWallet.setOnClickListener {
            if (loggedIn) {
                addToWallet()
            } else {
                Snackbar.make(view, "Login first", BaseTransientBottomBar.LENGTH_LONG).show()
            }
        }

        val authSdk = AuthSdkProvider.getInstance()

        val loginCallback = object : AuthSdk.LoginCallback {
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
                initCardsSdk()
            }

            override fun onFailure(message: String) {
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.loginBtn.setOnClickListener {
            if (userPhoneNumber.isBlank()) {
                authSdk.login(requireContext(), loginCallback)
            } else {
                authSdk.login(requireContext(), userPhoneNumber, loginCallback)
            }
        }

        requireActivity().runOnUiThread {
            cardsVM.cardsList.observe(viewLifecycleOwner) { list ->
                list?.let {
                    //TODO do something with the cards
                    if (it.isNotEmpty()) {
                        card = it[0]
                    }
                }
            }
        }
    }

    private fun initCardsSdk() {
        CardsSdkProvider.getInstance().init(requireActivity(),
            accessToken,
            debug = true,
            useFirebase =  false,
            object : CardsSdk.InvalidTokenCallback {
                override fun onTokenInvalid(): String {
                    return AuthSdkProvider.getInstance().refreshTokens().accessToken
                }
            })

        cardsSdk.setWalletCallback(object : CardsSdk.WalletCallback {
            override fun onDataChanged(liveData: LiveData<WalletResponse<WalletStatus>>) {
                observeSync(liveData)
            }
        })

        observeSync(cardsSdk.syncWallet())

        cardsVM.requestCardsList()
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
            //TODO add your card's portfolio name
            cardsSdk.getCardWalletStatus(card, "portfolio name").observe(viewLifecycleOwner) {
                updateCardStatus(it)
            }
        }
    }

    private fun addToWallet() {
        requireActivity().runOnUiThread {
            // result of this is caught in onActivityResult() but can be safely ignored
            cardsSdk.addCardToWallet(card).observe(viewLifecycleOwner) {
                updateCardStatus(it)
            }
        }
    }

    private fun activateCard() {
        requireActivity().runOnUiThread {
            // result of this is caught in onActivityResult() but can be safely ignored
            cardsSdk.activateCardInWallet(card).observe(viewLifecycleOwner) {
                updateCardStatus(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
