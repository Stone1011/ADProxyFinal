package ad.blocker.connect.feature.manager.view

import ad.blocker.MainActivity
import ad.blocker.R
import ad.blocker.connect.feature.manager.viewmodel.ProxyManagerViewModel
import ad.blocker.databinding.FragmentProxyManagerBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProxyManagerFragment : Fragment() {

    companion object {
        fun newInstance() = ProxyManagerFragment()
    }

    private val binding by lazy { FragmentProxyManagerBinding.inflate(layoutInflater) }
    private val viewModel: ProxyManagerViewModel by viewModels()

    private var dialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupIcons()

        observeProxyState()
        observeProxyEvent()
    }

    private fun observeProxyEvent() {
        viewModel.proxyEvent.observe(viewLifecycleOwner, Observer { proxyEvent ->
            hideErrors()
            when (proxyEvent) {
                is ProxyManagerEvent.InvalidAddress -> showInvalidAddressError()
                is ProxyManagerEvent.InvalidPort -> showInvalidPortError()
            }
        })
    }

    private fun observeProxyState() {
        viewModel.proxyState.observe(viewLifecycleOwner, Observer { proxyState ->
            when (proxyState)
            {
                is ProxyState.Enabled -> showProxyEnabled(proxyState.address, proxyState.port)
                is ProxyState.Disabled -> showProxyDisabled()
            }
        })
    }

    private fun setupIcons() {
        // Info icon
        binding.info.setOnClickListener {
            dialog?.dismiss()
            dialog = AlertDialog.Builder(requireContext())
                .setMessage(R.string.dialog_message_information)
                .setPositiveButton(getString(R.string.dialog_action_close)) { _, _ -> }
                .show()
        }

        // Theme mode icon
        binding.themeMode.setOnClickListener { viewModel.toggleTheme() }
    }

    private fun disableProxy() {
        MainActivity.first = false
        viewModel.disableProxy()
    }

    private fun showProxyEnabled(proxyAddress: String, proxyPort: String) {
        hideErrors()
        with(binding)
        {
            if(MainActivity.first)
            {
                disableProxy()
                return
            }

            inputLayoutAddress.editText?.setText(proxyAddress)
            inputLayoutPort.editText?.setText(proxyPort)
            rulesPath.setText("/storage/emulated/0/Download/rules.txt")

            inputLayoutAddress.isEnabled = false
            inputLayoutPort.isEnabled = false
            status.text = getString(R.string.proxy_status_enabled)
            status.isActivated = true
            toggle.isActivated = true
            toggle.contentDescription = getString(R.string.a11y_disable_proxy)
            toggle.setOnClickListener {
                viewModel.disableProxy()
            }

            val path = rulesPath.text.toString()
//            Log.e("rules", path)
            var success = MainActivity.filter.init(path)
            if(!success)
            {
//                showInvalidPathError()
                val dialog = AlertDialog.Builder(MainActivity.context)
                    .setTitle("Warning")
                    .setMessage(R.string.error_invalid_path)
                    .setCancelable(true).create()
                dialog.show()
            }
            else
                MainActivity.startProxy(context, Integer.parseInt(proxyPort))
        }
    }

    private fun showProxyDisabled() {
        with(binding) {
            val lastUsedProxy = viewModel.lastUsedProxy
            if (lastUsedProxy.isEnabled) {
                inputLayoutAddress.editText?.setText(lastUsedProxy.address)
                inputLayoutPort.editText?.setText(lastUsedProxy.port)
            }
            inputLayoutAddress.isEnabled = false
            inputLayoutPort.isEnabled = true
            status.text = getString(R.string.proxy_status_disabled)
            rulesPath.setText("/storage/emulated/0/Download/rules.txt")
            toggle.isActivated = false
            status.isActivated = false
            toggle.contentDescription = getString(R.string.a11y_enable_proxy)
            toggle.setOnClickListener {
                viewModel.enableProxy(
                    inputLayoutAddress.editText?.text?.toString().orEmpty(),
                    inputLayoutPort.editText?.text?.toString().orEmpty()
                )
            }
        }
    }

    private fun showInvalidAddressError() {
        binding.inputLayoutAddress.apply {
            error = getString(R.string.error_invalid_address)
            requestFocusFromTouch()
        }
    }

    private fun showInvalidPortError() {
        binding.inputLayoutPort.apply {
            error = getString(R.string.error_invalid_port)
            requestFocusFromTouch()
        }
    }

    private fun showInvalidPathError() {
        binding.inputLayoutPath.apply {
            error = getString(R.string.error_invalid_path)
            requestFocusFromTouch()
        }
    }

    private fun hideErrors() {
        with(binding) {
            inputLayoutAddress.error = null
            inputLayoutPort.error = null
        }
    }
}
