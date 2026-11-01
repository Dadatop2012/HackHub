package com.example.hackhub // <-- Sostituisci con il nome esatto del tuo package corrente!

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import  java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socketkotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow



class MainActivity : AppCompatActivity() {

    // Contenitori di Stato Principali e Menu
    private lateinit var layoutMainRoot: RelativeLayout
    private lateinit var layoutCalculator: LinearLayout
    private lateinit var layoutHackHub: LinearLayout
    private lateinit var layoutBottomMenu: LinearLayout
    private lateinit var btnSwitchTheme: Button
    private lateinit var txtHubTitle: TextView
    private lateinit var menuBtnRadar: Button
    private lateinit var menuBtnLan: Button
    private lateinit var menuBtnPorts: Button
    private lateinit var menuBtnDns: Button
    private lateinit var menuBtnBrowser: Button

    // Contenitori dei Singoli Moduli (Tab)
    private lateinit var tabWirelessRadar: LinearLayout
    private lateinit var tabLanScanner: LinearLayout
    private lateinit var tabPortScannerLayout: LinearLayout
    private lateinit var tabDnsRecon: LinearLayout
    private lateinit var tabBrowser: LinearLayout

    // Componenti della Calcolatrice di Copertura
    private lateinit var txtCalcDisplay: TextView
    private lateinit var txtCalcHistory: TextView
    private var currentInput = ""
    private val secretPin = "1234"

    // Componenti Modulo 1: Radar Wireless
    private lateinit var wifiManager: WifiManager
    private lateinit var btnScanWifi: Button
    private lateinit var txtWifiResults: TextView
    private var lastScanResults: List<ScanResult> = emptyList()
    private val expandedNetworkSsids = mutableSetOf<String>()
    private val channelCrowdMap = mutableMapOf<Int, Int>()
    private var connectedWifiSsid: String? = null


    // Componenti Modulo 2 e 3: LAN Scanner, Port Auditor & ISP Info
    private lateinit var txtIspProvider: TextView
    private lateinit var txtLocalIpInfo: TextView
    private lateinit var btnScanLan: Button

    private lateinit var editTxtPortTimeout: EditText

    private lateinit var editTxtTargetIpScan: EditText
    private lateinit var btnStartPortScan: Button
    private lateinit var editTxtPortStart: EditText
    private lateinit var editTxtPortEnd: EditText
    private lateinit var btnProfileWeb: Button
    private lateinit var btnProfileGaming: Button
    private lateinit var btnProfileComuni: Button
    private lateinit var editTxtMacTarget: EditText
    private lateinit var btnSendWol: Button
    private lateinit var txtLanResults: TextView
    private lateinit var txtPortResults: TextView
    private var myDeviceIpAddress = "0.0.0.0"

    // Componenti Modulo 4: DNS Recon
    private lateinit var editTxtDnsTarget: EditText
    private lateinit var btnStartDnsRecon: Button
    private lateinit var txtDnsResults: TextView

    // Componenti Modulo 5: Chrome Browser
    private lateinit var webView: WebView
    private lateinit var editTxtUrl: EditText
    private lateinit var webProgressBar: ProgressBar
    private lateinit var btnHome: Button
    private lateinit var btnRefresh: Button

    // Gestione Temi
    private var isDarkThemeActive = true // true = Cyberpunk (Nero/Verde), false = Minimal (Bianco/Viola)

    // Richiesta Permessi GPS Asincrona
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            startWifiScan()
        } else {
            txtWifiResults.text = "❌ Errore SIGINT: Permessi GPS negati. Mappatura aerea Wi-Fi interrotta."
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inizializzazione Struttura di Stato e Menu
        layoutMainRoot = findViewById(R.id.layoutMainRoot)
        layoutCalculator = findViewById(R.id.layoutCalculator)
        layoutHackHub = findViewById(R.id.layoutHackHub)
        layoutBottomMenu = findViewById(R.id.layoutBottomMenu)
        btnSwitchTheme = findViewById(R.id.btnSwitchTheme)
        txtHubTitle = findViewById(R.id.txtHubTitle)

        menuBtnRadar = findViewById(R.id.menuBtnRadar)
        menuBtnLan = findViewById(R.id.menuBtnLan)
        menuBtnPorts = findViewById(R.id.menuBtnPorts)
        menuBtnDns = findViewById(R.id.menuBtnDns)
        menuBtnBrowser = findViewById(R.id.menuBtnBrowser)

        // Abbinamento dei 5 Tab Layout
        tabWirelessRadar = findViewById(R.id.tabWirelessRadar)
        tabLanScanner = findViewById(R.id.tabLanScanner)
        tabPortScannerLayout = findViewById(R.id.tabPortScannerLayout)
        tabDnsRecon = findViewById(R.id.tabDnsRecon)
        tabBrowser = findViewById(R.id.tabBrowser)

        // 2. Inizializzazione Calcolatrice
        txtCalcDisplay = findViewById(R.id.txtCalcDisplay)
        txtCalcHistory = findViewById(R.id.txtCalcHistory)

        // 3. Inizializzazione Modulo 1 (Radar)
        btnScanWifi = findViewById(R.id.btnScanWifi)
        txtWifiResults = findViewById(R.id.txtWifiResults)
        txtWifiResults.movementMethod = LinkMovementMethod.getInstance()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // 4. Inizializzazione Modulo 2 e 3 (LAN / Porte)
        txtIspProvider = findViewById(R.id.txtIspProvider)
        txtLocalIpInfo = findViewById(R.id.txtLocalIpInfo)
        btnScanLan = findViewById(R.id.btnScanLan)
        editTxtTargetIpScan = findViewById(R.id.editTxtTargetIpScan)
        btnStartPortScan = findViewById(R.id.btnStartPortScan)
        editTxtPortStart = findViewById(R.id.editTxtPortStart)
        editTxtPortEnd = findViewById(R.id.editTxtPortEnd)
        btnProfileWeb = findViewById(R.id.btnProfileWeb)
        btnProfileGaming = findViewById(R.id.btnProfileGaming)
        btnProfileComuni = findViewById(R.id.btnProfileComuni)
        editTxtMacTarget = findViewById(R.id.editTxtMacTarget)
        btnSendWol = findViewById(R.id.btnSendWol)
        txtLanResults = findViewById(R.id.txtLanResults)
        txtPortResults = findViewById(R.id.txtPortResults)
        editTxtPortTimeout = findViewById(R.id.editTxtPortTimeout)


        // 5. Inizializzazione Modulo 4 (DNS)
        editTxtDnsTarget = findViewById(R.id.editTxtDnsTarget)
        btnStartDnsRecon = findViewById(R.id.btnStartDnsRecon)
        txtDnsResults = findViewById(R.id.txtDnsResults)

        // 6. Inizializzazione Modulo 5 (Browser Chrome)
        webView = findViewById(R.id.webView)
        editTxtUrl = findViewById(R.id.editTxtUrl)
        webProgressBar = findViewById(R.id.webProgressBar)
        btnHome = findViewById(R.id.btnHome)
        btnRefresh = findViewById(R.id.btnRefresh)

        // Attivazione dei trigger logici di sistema
        setupCalculatorLogic()
        setupBottomNavigationMenu()
        setupPortProfileButtons()
        setupDnsReconLogic()
        setupSimpleChromeLogic()

        btnSwitchTheme.setOnClickListener { toggleHackHubTheme() }
        setupAntipanicoBackDispatcher()
    }

    private fun setupBottomNavigationMenu() {
        val tabSwitch = { rVis: Int, lVis: Int, pVis: Int, dVis: Int, bVis: Int ->
            tabWirelessRadar.visibility = rVis
            tabLanScanner.visibility = lVis
            tabPortScannerLayout.visibility = pVis
            tabDnsRecon.visibility = dVis
            tabBrowser.visibility = bVis
        }

        menuBtnRadar.setOnClickListener {
            tabSwitch(View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE)
            updateMenuButtonFocus(menuBtnRadar, listOf(menuBtnLan, menuBtnPorts, menuBtnDns, menuBtnBrowser))
        }
        menuBtnLan.setOnClickListener {
            tabSwitch(View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE)
            updateMenuButtonFocus(menuBtnLan, listOf(menuBtnRadar, menuBtnPorts, menuBtnDns, menuBtnBrowser))
            autoFetchNetworkDetailsAndPrecomplete()
        }
        menuBtnPorts.setOnClickListener {
            tabSwitch(View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE)
            updateMenuButtonFocus(menuBtnPorts, listOf(menuBtnRadar, menuBtnLan, menuBtnDns, menuBtnBrowser))
            autoFetchNetworkDetailsAndPrecomplete()
        }
        menuBtnDns.setOnClickListener {
            tabSwitch(View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE)
            updateMenuButtonFocus(menuBtnDns, listOf(menuBtnRadar, menuBtnLan, menuBtnPorts, menuBtnBrowser))
        }
        menuBtnBrowser.setOnClickListener {
            tabSwitch(View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE)
            updateMenuButtonFocus(menuBtnBrowser, listOf(menuBtnRadar, menuBtnLan, menuBtnPorts, menuBtnDns))
            if (webView.url.isNullOrEmpty()) webView.loadUrl("https://google.com")
        }
    }

    private fun updateMenuButtonFocus(active: Button, idles: List<Button>) {
        val focusBg = if (isDarkThemeActive) Color.parseColor("#1C1C24") else Color.parseColor("#6A1B9A")
        val focusText = if (isDarkThemeActive) Color.parseColor("#00E676") else Color.WHITE

        active.backgroundTintList = ColorStateList.valueOf(focusBg)
        active.setTextColor(focusText)

        val idleBg = if (isDarkThemeActive) Color.parseColor("#111115") else Color.parseColor("#E5E5EA")
        val idleText = if (isDarkThemeActive) Color.parseColor("#888888") else Color.parseColor("#777777")

        for (btn in idles) {
            btn.backgroundTintList = ColorStateList.valueOf(idleBg)
            btn.setTextColor(idleText)
        }
    }


    // INTERRUTTORE DI TEMA REAL-TIME: Ridisegna al volo l'aspetto cromatico e l'interfaccia a 5 schede
    private fun toggleHackHubTheme() {
        isDarkThemeActive = !isDarkThemeActive

        val bgColor = if (isDarkThemeActive) Color.parseColor("#0A0A0C") else Color.parseColor("#FFFFFF")
        val panelColor = if (isDarkThemeActive) Color.parseColor("#0A0A0C") else Color.parseColor("#F5F5F5")
        val textColor = if (isDarkThemeActive) Color.parseColor("#00E676") else Color.parseColor("#000000")
        val inputBg = if (isDarkThemeActive) Color.parseColor("#1C1C24") else Color.parseColor("#E5E5EA")
        val inputTextColor = if (isDarkThemeActive) Color.WHITE else Color.BLACK
        val btnTint = if (isDarkThemeActive) Color.parseColor("#1C1C24") else Color.parseColor("#6A1B9A")
        val titleColor = if (isDarkThemeActive) Color.parseColor("#00E676") else Color.parseColor("#6A1B9A")

        layoutHackHub.setBackgroundColor(bgColor)
        layoutMainRoot.setBackgroundColor(bgColor)
        txtHubTitle.setTextColor(titleColor)

        val textViews = listOf(txtWifiResults, txtLanResults, txtPortResults, txtDnsResults)
        for (tv in textViews) {
            tv.setTextColor(textColor)
            (tv.parent as? View)?.setBackgroundColor(panelColor)
        }

        val inputs = listOf(editTxtTargetIpScan, editTxtPortStart, editTxtPortEnd, editTxtMacTarget, editTxtDnsTarget, editTxtUrl)
        for (input in inputs) {
            input.backgroundTintList = ColorStateList.valueOf(inputBg)
            input.setTextColor(inputTextColor)
        }

        val buttons = listOf(btnScanWifi, btnScanLan, btnStartPortScan, btnSendWol, btnStartDnsRecon, btnProfileWeb, btnProfileGaming, btnProfileComuni, btnSwitchTheme, btnHome, btnRefresh)
        for (btn in buttons) {
            btn.backgroundTintList = ColorStateList.valueOf(btnTint)
            btn.setTextColor(Color.WHITE)
        }

        val menuBg = if (isDarkThemeActive) Color.parseColor("#111115") else Color.parseColor("#E5E5EA")
        layoutBottomMenu.setBackgroundColor(menuBg)

        val activeBtn = when {
            tabWirelessRadar.visibility == View.VISIBLE -> menuBtnRadar
            tabLanScanner.visibility == View.VISIBLE -> menuBtnLan
            tabPortScannerLayout.visibility == View.VISIBLE -> menuBtnPorts
            tabDnsRecon.visibility == View.VISIBLE -> menuBtnDns
            else -> menuBtnBrowser
        }
        updateMenuButtonFocus(activeBtn, listOf(menuBtnRadar, menuBtnLan, menuBtnPorts, menuBtnDns, menuBtnBrowser).filter { it != activeBtn })
    }


    private fun resetCalculatorState() {
        currentInput = "0"
        txtCalcDisplay.text = currentInput
        txtCalcHistory.text = ""
    }

    private fun setupCalculatorLogic() {
        val buttonsMap = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7",
            R.id.btn8 to "8", R.id.btn9 to "9", R.id.btnAdd to "+", R.id.btnSub to "-",
            R.id.btnMul to "*", R.id.btnDiv to "/", R.id.btnDot to "."
        )
        for ((id, value) in buttonsMap) {
            findViewById<Button>(id).setOnClickListener {
                if (currentInput == "0") currentInput = ""
                currentInput += value
                txtCalcDisplay.text = currentInput
            }
        }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (currentInput.isNotEmpty() && currentInput != "0") {
                currentInput = currentInput.substring(0, currentInput.length - 1)
                if (currentInput.isEmpty()) currentInput = "0"
                txtCalcDisplay.text = currentInput
            }
        }
        findViewById<Button>(R.id.btnC).setOnClickListener { resetCalculatorState() }
        findViewById<Button>(R.id.btnEqual).setOnClickListener {
            if (currentInput == secretPin) {
                layoutCalculator.visibility = View.GONE
                layoutHackHub.visibility = View.VISIBLE
                layoutBottomMenu.visibility = View.VISIBLE
                autoFetchNetworkDetailsAndPrecomplete() // Sincronizza subito i parametri di rete
            } else {
                executeRealCoverMath()
            }
        }
    }

    private fun executeRealCoverMath() {
        try {
            val input = currentInput
            val op = when {
                input.contains("+") -> "+"
                input.contains("-") -> "-"
                input.contains("*") -> "*"
                input.contains("/") -> "/"
                else -> ""
            }
            if (op.isNotEmpty()) {
                val parts = input.split(op)
                if (parts.size == 2) {
                    val n1 = parts[0].toDoubleOrNull() ?: 0.0
                    val n2 = parts[1].toDoubleOrNull() ?: 0.0
                    val res = when (op) {
                        "+" -> n1 + n2
                        "-" -> n1 - n2
                        "*" -> n1 * n2
                        "/" -> if (n2 != 0.0) n1 / n2 else Double.NaN
                        else -> 0.0
                    }
                    txtCalcHistory.text = "$input ="
                    currentInput = res.toString().removeSuffix(".0")
                    txtCalcDisplay.text = currentInput
                }
            }
        } catch (e: Exception) { txtCalcDisplay.text = "Errore" }
    }

    // ================= LOGICA MODULO 1: WIRELESS RADAR FORENSIC =================

    private fun setupWirelessRadarLogic() {
        btnScanWifi.setOnClickListener {
            val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                startWifiScan()
            } else {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }

    private fun startWifiScan() {
        txtWifiResults.text = "Intercettazione pacchetti Beacon aerei in corso..."
        val info = wifiManager.connectionInfo
        connectedWifiSsid = info?.ssid?.replace("\"", "")

        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    lastScanResults = wifiManager.scanResults
                    renderExpandableWifiList()
                }
                try { unregisterReceiver(this) } catch (e: Exception) {}
            }
        }
        registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
    }
    private fun setupAntipanicoBackDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tabBrowser.visibility == View.VISIBLE) {
                    // Se l'utente usa il tasto indietro nativo mentre naviga, esce dal browser a schermo intero
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        webView.loadUrl("about:blank")
                        tabBrowser.visibility = View.GONE

                        // Ripristina la visibilità dell'interfaccia HackHub e del menu inferiore
                        layoutBottomMenu.visibility = View.VISIBLE
                        tabWirelessRadar.visibility = View.VISIBLE
                        updateMenuButtonFocus(menuBtnRadar, listOf(menuBtnLan, menuBtnPorts, menuBtnDns, menuBtnBrowser))
                    }
                } else if (layoutHackHub.visibility == View.VISIBLE) {
                    // Se è nella dashboard normale, torna alla calcolatrice
                    layoutHackHub.visibility = View.GONE
                    layoutBottomMenu.visibility = View.GONE
                    layoutCalculator.visibility = View.VISIBLE
                    resetCalculatorState()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun renderExpandableWifiList() {
        if (lastScanResults.isEmpty()) {
            txtWifiResults.text = "Nessuna rete Wi-Fi rilevata nelle vicinanze."
            return
        }
        channelCrowdMap.clear()
        for (scan in lastScanResults) {
            val ch = convertFrequencyToChannel(scan.frequency)
            channelCrowdMap[ch] = (channelCrowdMap[ch] ?: 0) + 1
        }
        val sortedResults = lastScanResults.sortedByDescending { it.level }
        val builder = SpannableStringBuilder()
        builder.append(Html.fromHtml("<h3>WIRELESS RADAR FORENSIC ANALYZER: ${sortedResults.size} RETI</h3><br>", Html.FROM_HTML_MODE_LEGACY))

        for (scan in sortedResults) {
            val ssid = if (scan.SSID.isNullOrEmpty()) "Rete Nascosta" else scan.SSID
            val isExpanded = expandedNetworkSsids.contains(ssid)
            val prefixText = if (isExpanded) "[V] RETE: $ssid\n" else "[>] RETE: $ssid\n"
            val startIndex = builder.length

            val nameColor = if (isDarkThemeActive) "#00E676" else "#6A1B9A"
            builder.append(Html.fromHtml("<font color='$nameColor'><b>$prefixText</b></font>", Html.FROM_HTML_MODE_LEGACY))
            val endIndex = builder.length

            builder.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    if (expandedNetworkSsids.contains(ssid)) expandedNetworkSsids.remove(ssid) else expandedNetworkSsids.add(ssid)
                    renderExpandableWifiList()
                }
            }, startIndex, endIndex - 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (isExpanded) {
                val subBuilder = StringBuilder()
                val signalDbm = scan.level
                val freqMhz = scan.frequency
                val channel = convertFrequencyToChannel(freqMhz)
                val bandType = if (freqMhz in 4900..5900) "5.0 GHz" else "2.4 GHz"

                val signalColor = when { signalDbm >= -55 -> "#00E676"
                    signalDbm in -70..-56 -> "#FFD600"
                    else -> "#FF3D00"
                }
                val devOnCh = channelCrowdMap[channel] ?: 1
                val crowdText = when { devOnCh >= 5 -> "<font color='#FF3D00'>Affollato ($devOnCh reti)</font>"
                    devOnCh in 2..4 -> "<font color='#FFD600'>Moderato ($devOnCh reti)</font>"
                    else -> "<font color='#00E676'>Libero (Canale Ottimale)</font>"
                }

                val capabilitiesStr = scan.capabilities ?: ""
                val securityText = detectSecurity(capabilitiesStr)
                val distanceStr = "${String.format("%.1f", calculateDistance(signalDbm, freqMhz))} metri"
                val techVersion = detectWifiTechnology(scan)
                val bssidMac = scan.BSSID ?: "Sconosciuto"
                val vendorName = lookupVendor(bssidMac)
                val bandwidthStr = detectBandwidth(scan)
                val uptimeStr = calculateUptime(scan.timestamp)

                // INTELLIGENCE AVANZATA RED TEAM SATELLITI
                val pmfAudit = checkDeauthVulnerability(capabilitiesStr)
                val wpsIndex = calculateWpsExploitIndex(capabilitiesStr, bssidMac)
                val meshTree = analyzeNetworkRelationships(ssid, bssidMac, sortedResults)

                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Sicurezza: $securityText<br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Generazione: $techVersion<br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;BSSID MAC: <b>$bssidMac</b><br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Vendor Italia: <b>$vendorName</b><br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Ampiezza (Bandwidth): <b>$bandwidthStr</b><br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Uptime Modem: $uptimeStr<br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Potenza Segnale: <font color='$signalColor'><b>$signalDbm dBm</b></font> (Distanza: $distanceStr)<br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Banda: $bandType | Canale: <b>$channel</b> ($crowdText)<br>")

                // SEZIONE ANALISI AVANZATA NEON
                subBuilder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;<font color='#FF9100'><b>[RED TEAM AUDIT METRICS]</b></font><br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Vulnerabilita Deauth (PMF): $pmfAudit<br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Vulnerabilita WPS (Pixie-Dust): $wpsIndex<br>")
                subBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Relazioni Spazio Aereo: $meshTree<br>")

                if (ssid == connectedWifiSsid) {
                    subBuilder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;<font color='#00E676'><b>[CLICCA QUI PER SCANSIONARE GLI IP DI QUESTA RETE]</b></font><br>")
                }

                val detailStartIndex = builder.length
                builder.append(Html.fromHtml(subBuilder.toString(), Html.FROM_HTML_MODE_LEGACY))

                if (ssid == connectedWifiSsid) {
                    val linkText = "[CLICCA QUI PER SCANSIONARE GLI IP DI QUESTA RETE]"
                    val subStr = builder.toString()
                    val clickLinkStart = subStr.indexOf(linkText, detailStartIndex)
                    if (clickLinkStart != -1) {
                        builder.setSpan(object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                tabWirelessRadar.visibility = View.GONE
                                tabLanScanner.visibility = View.VISIBLE
                                updateMenuButtonFocus(menuBtnLan, listOf(menuBtnRadar, menuBtnPorts, menuBtnDns, menuBtnBrowser))
                                autoFetchNetworkDetailsAndPrecomplete()
                                startSubnetIpScannerEngine()
                            }
                        }, clickLinkStart, clickLinkStart + linkText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
            val dividerColor = if (isDarkThemeActive) "#333333" else "#CCCCCC"
            builder.append(Html.fromHtml("<font color='$dividerColor'>--------------------------------------------------</font><br>", Html.FROM_HTML_MODE_LEGACY))
        }
        txtWifiResults.text = builder
    }

    private fun checkDeauthVulnerability(capabilities: String): String {
        val capUpper = capabilities.uppercase()
        return when {
            capUpper.contains("MFPR") || capUpper.contains("WPA3") -> "<font color='#00E676'>Protetto (PMF Obbligatorio)</font>"
            capUpper.contains("MFPC") -> "<font color='#FFD600'>Parziale (PMF Opzionale)</font>"
            else -> "<font color='#FF3D00'><b>VULNERABILE (Nessun PMF - Esposto a Deauth)</b></font>"
        }
    }

    private fun calculateWpsExploitIndex(capabilities: String, bssid: String): String {
        if (!capabilities.contains("WPS", ignoreCase = true)) return "<font color='#00E676'>Sicuro (WPS Disattivato)</font>"
        val oui = bssid.replace(":", "").substring(0, 6).uppercase()
        val vulnerableOuiList = listOf("50C7BF", "98DED0", "0014D1", "001882", "24DF6A", "A0E4CB")
        return if (vulnerableOuiList.contains(oui)) "<font color='#FF3D00'><b>ALTO RISCHIO (Chipset vulnerabile a Pixie-Dust)</b></font>"
        else "<font color='#FFD600'>Medio (WPS Attivo)</font>"
    }

    private fun analyzeNetworkRelationships(currentSsid: String, currentBssid: String, allNetworks: List<ScanResult>): String {
        if (currentBssid.length < 14) return "Indipendente"
        val currentPrefix = currentBssid.substring(0, 14).uppercase()
        for (scan in allNetworks) {
            val otherBssid = scan.BSSID ?: ""
            val otherSsid = if (scan.SSID.isNullOrEmpty()) "Nascosta" else scan.SSID
            if (otherBssid.uppercase() != currentBssid.uppercase() && otherBssid.uppercase().startsWith(currentPrefix)) {
                return if (otherSsid == currentSsid) "<font color='#FFD600'>Nodo Mesh/Ripetitore (Stesso SSID)</font>"
                else "<font color='#00B0FF'>Rete Ospiti / SSID Gemello ($otherSsid)</font>"
            }
        }
        return "Nessuna relazione aerea rilevata"
    }



    // ================= LOGICA MODULO 2 & 3: ADATTIVO LAN & PORT SCANNING =================

    private fun autoFetchNetworkDetailsAndPrecomplete() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val linkProps = connectivityManager.getLinkProperties(activeNetwork)

        myDeviceIpAddress = "0.0.0.0"
        linkProps?.linkAddresses?.forEach { linkAddress ->
            val address = linkAddress.address
            if (address is Inet4Address && !address.isLoopbackAddress) {
                myDeviceIpAddress = address.hostAddress ?: "0.0.0.0"
            }
        }
        txtLocalIpInfo.text = "Mio IP: $myDeviceIpAddress"
        if (editTxtTargetIpScan.text.toString().isEmpty()) {
            editTxtTargetIpScan.setText(myDeviceIpAddress)
        }

        var ispName = "Rete Privata Locale"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val info = wifiManager.connectionInfo
            val ssidUpper = info?.ssid?.uppercase() ?: ""
            when {
                ssidUpper.contains("TIM") || ssidUpper.contains("TELECOM") -> ispName = "TIM 🇮🇹"
                ssidUpper.contains("VODAFONE") || ssidUpper.contains("OMNITEL") -> ispName = "Vodafone 🇮🇹"
                ssidUpper.contains("FASTWEB") -> ispName = "Fastweb 🇮🇹"
                ssidUpper.contains("WINDTRE") || ssidUpper.contains("WIND") -> ispName = "WindTre 🇮🇹"
                ssidUpper.contains("ILIAD") -> ispName = "Iliad 🇮🇹"
                ssidUpper.contains("POSTE") -> ispName = "PosteMobile 🇮🇹"
                ssidUpper.contains("SKY") -> ispName = "Sky Wifi 🇮🇹"
            }
        }
        txtIspProvider.text = "ISP: $ispName"
    }

    private fun setupPortProfileButtons() {
        btnScanLan.setOnClickListener { startSubnetIpScannerEngine() }

        btnProfileWeb.setOnClickListener {
            editTxtPortStart.setText("80")
            editTxtPortEnd.setText("443")
        }
        btnProfileGaming.setOnClickListener {
            editTxtPortStart.setText("3074")
            editTxtPortEnd.setText("3479")
        }
        btnProfileComuni.setOnClickListener {
            editTxtPortStart.setText("21")
            editTxtPortEnd.setText("445")
        }

        btnStartPortScan.setOnClickListener {
            val targetIp = editTxtTargetIpScan.text.toString().trim()
            val startPortStr = editTxtPortStart.text.toString().trim()
            val endPortStr = editTxtPortEnd.text.toString().trim()
            val timeoutStr = editTxtPortTimeout.text.toString().trim()

            if (targetIp.isNotEmpty() && startPortStr.isNotEmpty() && endPortStr.isNotEmpty()) {
                val start = startPortStr.toIntOrNull() ?: 1
                val end = endPortStr.toIntOrNull() ?: 1024

                // RILEVA TIMEOUT UTENTE: Se la casella è vuota o errata, applica il valore standard di 800ms
                val customTimeout = timeoutStr.toIntOrNull() ?: 800

                startAdvancedPortScannerEngine(targetIp, start, end, customTimeout)
            } else {
                txtPortResults.text = "❌ Errore: Compila l'IP target e un intervallo di porte valido."
            }
        }

        btnSendWol.setOnClickListener {
            val macStr = editTxtMacTarget.text.toString().trim()
            if (macStr.isNotEmpty()) {
                sendWakeOnLanPacket(macStr)
            } else {
                txtPortResults.text = "❌ Inserisci un MAC address valido per inviare la scossa WOL."
            }
        }
        setupWirelessRadarLogic()
    }

    private fun startAdvancedPortScannerEngine(targetIp: String, startPort: Int, endPort: Int, userTimeout: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                txtPortResults.text = "🚪 PORT AUDITING SU: $targetIp\nIntervallo: $startPort ➔ $endPort\nAttesa massima impostata: $userTimeout ms...\n\n"
            }

            val portsToScan = (startPort..endPort).toList()
            val openPorts = java.lang.StringBuilder()
            val overtimingPorts = java.lang.StringBuilder()
            val closedPorts = java.lang.StringBuilder()

            val chunkedPorts = portsToScan.chunked(25)

            for (chunk in chunkedPorts) {
                val jobs = chunk.map { port ->
                    async {
                        val socket = Socket()
                        try {
                            // APPLICA IL TIMEOUT DINAMICO SCELTO DALL'UTENTE
                            socket.connect(InetSocketAddress(targetIp, port), userTimeout)

                            var bannerText = "Nessuna firma inviata"
                            try {
                                socket.soTimeout = 400
                                val writer = PrintWriter(socket.getOutputStream(), true)
                                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                                writer.println()
                                val line = reader.readLine()
                                if (!line.isNullOrEmpty()) bannerText = line.trim()
                                socket.close()
                            } catch (e: Exception) {
                                bannerText = getDefaultServiceDescription(port)
                            }

                            synchronized(openPorts) {
                                openPorts.append("<font color='#00E676'><b>🔓 PORTA $port OPEN</b></font> ➔ $bannerText<br>")
                            }
                        } catch (e: java.net.SocketTimeoutException) {
                            // SE LA PORTA NON RISPONDE ENTRO I MILLISECONDI DEFINITI DALL'UTENTE
                            synchronized(overtimingPorts) {
                                overtimingPorts.append("<font color='#FF9100'><b>⏱️ PORTA $port OVERTIMING</b></font> (Nessuna risposta in $userTimeout ms)<br>")
                            }
                        } catch (e: Exception) {
                            synchronized(closedPorts) {
                                closedPorts.append("<font color='#777777'>🔒 PORTA $port CLOSED</font><br>")
                            }
                        } finally {
                            try { socket.close() } catch (e: Exception) {}
                        }
                    }
                }
                jobs.awaitAll()
            }

            withContext(Dispatchers.Main) {
                val finalReport = java.lang.StringBuilder()
                finalReport.append("<h3>🚪 REPORT ANALISI RISPOSTE PORTE:</h3><br>")

                if (openPorts.isNotEmpty()) {
                    finalReport.append("<b>[SERVIZI ATTIVI DETECTED]</b><br>$openPorts<br>")
                }
                if (overtimingPorts.isNotEmpty()) {
                    finalReport.append("<b>[NON HA RISPOSTO IN TEMPO (OVERTIMING)]</b><br>$overtimingPorts<br>")
                }
                if (closedPorts.isNotEmpty()) {
                    finalReport.append("<b>[PORTE CHIUSE CON RIFIUTO NETTO]</b><br>$closedPorts")
                }

                if (openPorts.isEmpty() && overtimingPorts.isEmpty() && closedPorts.isEmpty()) {
                    txtPortResults.text = "Nessun dato estratto dall'intervallo."
                } else {
                    txtPortResults.text = Html.fromHtml(finalReport.toString(), Html.FROM_HTML_MODE_LEGACY)
                }
            }
        }
    }



    private fun getDefaultServiceDescription(port: Int): String {
        return when(port) {
            21 -> "FTP File Transfer"
            22 -> "SSH Secure Shell Terminal"
            23 -> "Telnet Plain Text"
            80 -> "HTTP Web Server"
            443 -> "HTTPS Secure Web Server"
            445 -> "SMB Windows File Share"
            502 -> "Modbus Industrial PLC IoT ⚠️"
            1883 -> "MQTT Domotica Centralina IoT ⚠️"
            3074, 3478, 3479 -> "Gaming Console Service (NAT)"
            8080 -> "HTTP Proxy Server / Admin Panel"
            else -> "Servizio Generico"
        }
    }
    // Scanner Sottorete LAN parallelo: estrae sia l'indirizzo IP che il Nome del Dispositivo (HostName) per riconoscerli
    private fun startSubnetIpScannerEngine() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                txtLanResults.text = "Inizializzazione scansione IP di sottorete...\n"
            }

            if (myDeviceIpAddress == "0.0.0.0" || myDeviceIpAddress.isEmpty()) {
                withContext(Dispatchers.Main) {
                    txtLanResults.text = "❌ Errore LAN: Connettiti al Wi-Fi per mappare gli IP locali."
                }
                return@launch
            }

            val lastDotIndex = myDeviceIpAddress.lastIndexOf('.')
            val subnetPrefix = myDeviceIpAddress.substring(0, lastDotIndex + 1)

            withContext(Dispatchers.Main) {
                txtLanResults.text = "🔍 SCANSIONE DISPOSITIVI SU SOTTORETE: ${subnetPrefix}*\nAnalisi identità host in corso...\n\n"
            }

            val foundDevices = mutableListOf<Pair<String, String>>()

            // Ciclo asincrono parallelo su tutti i 254 indirizzi LAN tramite Coroutine
            val jobs = (1..254).map { i ->
                async {
                    val testIp = subnetPrefix + i
                    try {
                        val address = InetAddress.getByName(testIp)
                        // Verifica se l'host risponde entro 220 millisecondi per stabilità wireless
                        if (address.isReachable(220)) {
                            val hostName = address.canonicalHostName
                            val displayName = if (hostName == testIp) "Host Sconosciuto" else hostName
                            synchronized(foundDevices) {
                                foundDevices.add(Pair(testIp, displayName))
                            }
                        }
                    } catch (e: Exception) { }
                }
            }
            jobs.awaitAll()

            withContext(Dispatchers.Main) {
                val sb = java.lang.StringBuilder()
                sb.append("<h3>💻 MAPPA STRUMENTI CONNESSI ALLA LAN:</h3><br>")
                if (foundDevices.isEmpty()) {
                    sb.append("Nessun dispositivo ha risposto ai pacchetti di rete.")
                } else {
                    val sortedDevices = foundDevices.sortedBy { dev ->
                        dev.first.substring(dev.first.lastIndexOf('.') + 1).toIntOrNull() ?: 0
                    }
                    for (dev in sortedDevices) {
                        val ip = dev.first
                        val name = dev.second

                        // Creazione del Link ipertestuale sul nome: cliccandoci compila l'IP sul Port Scanner (Tab 3)!
                        if (ip == myDeviceIpAddress) {
                            sb.append("<font color='#00E676'>👉 <b>$ip</b></font> ➔ [Questo smartphone Oppo A78 📱]<br>")
                        } else {
                            sb.append("<font color='#00B0FF'>👉 <b>$ip</b></font> ➔ Nome Dispositivo: <b>$name</b><br>")
                        }
                    }
                }
                txtLanResults.text = Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
            }
        }
    }

    // Trasmissione del pacchetto magico UDP sulla porta 9 per accendere i computer spenti a distanza (Wake on LAN)
    private fun sendWakeOnLanPacket(macStr: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val macBytes = macStr.split(":", "-").map { it.toInt(16).toByte() }.toByteArray()
                if (macBytes.size != 6) {
                    withContext(Dispatchers.Main) { txtPortResults.text = "❌ Formato MAC errato. Usa AA:BB:CC:DD:EE:FF" }
                    return@launch
                }

                // Creazione del frame di sferzata energetica: 6 byte di FF seguiti da 16 ripetizioni del MAC del target
                val bytes = ByteArray(6 + 16 * macBytes.size)
                for (i in 0..5) bytes[i] = 0xff.toByte()
                for (i in 1..16) {
                    System.arraycopy(macBytes, 0, bytes, 6 + i * macBytes.size - macBytes.size, macBytes.size)
                }

                val address = InetAddress.getByName("255.255.255.255") // Invio in Broadcast totale sulla rete
                val packet = java.net.DatagramPacket(bytes, bytes.size, address, 9)
                val socket = java.net.DatagramSocket()
                socket.send(packet)
                socket.close()

                withContext(Dispatchers.Main) {
                    txtPortResults.text = Html.fromHtml("<font color='#00E676'><b>⚡ MAGIC PACKET TRANSMITTED SUCCESSFUL!</b></font><br>Scossa d'accensione inviata in broadcast al MAC: $macStr sulla Porta 9 UDP.", Html.FROM_HTML_MODE_LEGACY)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { txtPortResults.text = "❌ Errore durante la trasmissione WOL: ${e.message}" }
            }
        }
    }

    // ================= LOGICA MODULO 4: DNS LOOKUP & RECON (OSINT) =================

    private fun setupDnsReconLogic() {
        btnStartDnsRecon.setOnClickListener {
            val targetDomain = editTxtDnsTarget.text.toString().trim().replace("http://", "").replace("https://", "")
            if (targetDomain.isNotEmpty()) {
                startDnsReconEngine(targetDomain)
            } else {
                txtDnsResults.text = "❌ Errore: Inserisci un dominio valido (es. google.com)."
            }
        }
    }

    private fun startDnsReconEngine(domain: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                txtDnsResults.text = "🔍 OSINT RECON SU: $domain\nInterrogazione record globali DNS...\n\n"
            }

            val sb = java.lang.StringBuilder()
            sb.append("<h3>🔍 REPORT OSINT DI INFRASTRUTTURA: $domain</h3><br>")

            try {
                val addresses = InetAddress.getAllByName(domain)
                sb.append("<font color='#00E676'><b>🌐 INDIRIZZI IP REALI (Record A):</b></font><br>")
                for (addr in addresses) {
                    sb.append("&nbsp;&nbsp;&nbsp;&nbsp;➔ IP: ${addr.hostAddress}<br>")
                }
                sb.append("<br>")

                val subdomainsDict = listOf("admin", "dev", "vpn", "mail", "ftp", "cpanel", "secure", "shop")
                sb.append("<font color='#FF9100'><b>👁️ MAPPA SOTTODOMINI COPERTI (Brute-Force):</b></font><br>")
                var subdomainsFound = 0

                val jobs = subdomainsDict.map { sub ->
                    async {
                        val testUrl = "$sub.$domain"
                        try {
                            val addr = InetAddress.getByName(testUrl)
                            synchronized(sb) {
                                sb.append("&nbsp;&nbsp;&nbsp;&nbsp;🟢 Rilevato: <b>$testUrl</b> ➔ IP: ${addr.hostAddress}<br>")
                                subdomainsFound++
                            }
                        } catch (e: Exception) { }
                    }
                }
                jobs.awaitAll()
                if (subdomainsFound == 0) sb.append("&nbsp;&nbsp;&nbsp;&nbsp;Nessun sottodominio comune esposto pubblicamente.<br>")

            } catch (e: Exception) {
                sb.append("<font color='#FF3D00'>❌ Errore di risoluzione: Dominio non raggiungibile o protetto da Firewall DNS.</font>")
            }

            withContext(Dispatchers.Main) {
                txtDnsResults.text = Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
            }
        }
    }

    // ================= LOGICA MODULO 5: CHROME BROWSER MULTI-ACCOUNT PERMANENTE =================

    private fun setupSimpleChromeLogic() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                webProgressBar.visibility = View.VISIBLE
                editTxtUrl.setText(url)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webProgressBar.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                webProgressBar.progress = newProgress
            }
        }

        editTxtUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                var urlInput = editTxtUrl.text.toString().trim()
                if (urlInput.isNotEmpty()) {
                    if (!urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                        urlInput = if (urlInput.contains(".") && !urlInput.contains(" ")) "https://$urlInput" else "https://google.com"
                    }
                    webView.loadUrl(urlInput)
                }
                true
            } else { false }
        }

        btnHome.setOnClickListener { webView.loadUrl("https://google.com") }
        btnRefresh.setOnClickListener { webView.reload() }

        menuBtnBrowser.setOnClickListener {
            // COMPORTAMENTO A SCHERMO INTERO: Nasconde la barra inferiore dei menu quando si attiva Chrome
            tabWirelessRadar.visibility = View.GONE
            tabLanScanner.visibility = View.GONE
            tabPortScannerLayout.visibility = View.GONE
            tabDnsRecon.visibility = View.GONE
            layoutBottomMenu.visibility = View.GONE // Spaisce il menu di navigazione dell'app!

            tabBrowser.visibility = View.VISIBLE
            if (webView.url.isNullOrEmpty()) webView.loadUrl("https://google.com")
        }
    }

    // ================= ALGORITMI DI SUPPORTO DI INTELLIGENCE RADIO =================

    private fun detectSecurity(capabilities: String): String {
        val capUpper = capabilities.uppercase()
        return when {
            capUpper.contains("SAE") || capUpper.contains("WPA3") -> "<font color='#00E676'><b>WPA3 (Massimo Grado)</b></font>"
            capUpper.contains("WPA2") || capUpper.contains("RSN") -> "<font color='#00E676'><b>WPA2 (Standard Sicuro)</b></font>"
            capUpper.contains("WPA") -> "<font color='#FFD600'>WPA (Vulnerabile/Vecchio)</font>"
            capUpper.contains("WEP") -> "<font color='#FF3D00'><b>WEP (Altamente Rischioso!)</b></font>"
            else -> "<font color='#FFD600'>Aperta / Mista</font>"
        }
    }

    private fun lookupVendor(bssid: String): String {
        if (bssid.length < 8) return "Sconosciuto"
        val oui = bssid.replace(":", "").substring(0, 6).uppercase()
        val vendorMap = mapOf(
            "3C3A73" to "AVM Fritz!Box 🇩🇪", "0024FE" to "AVM Fritz!Box 🇩🇪", "CCCEB1" to "AVM Fritz!Box 🇩🇪",
            "001882" to "Huawei (Vodafone/Mista) 🇨🇳", "24DF6A" to "Huawei 🇨🇳",
            "001A2B" to "Technicolor (TIM Hub / Fastweb) 🇮🇹", "A4B1E9" to "Technicolor 🇮🇹",
            "A0E4CB" to "ZTE (OpenFiber UltraFibra) 🇨🇳",
            "50C7BF" to "TP-Link Router 🌐", "98DED0" to "TP-Link 🌐",
            "C04A00" to "Netgear Pro 🇺🇸", "BC1401" to "Asus Router 🇹🇼",
            "E4C722" to "Sagemcom (Vodafone Station) 🇬🇧", "140C76" to "Sagemcom 🇫🇷",
            "04C5A4" to "Askey (Sky Wifi Box) 🇬🇧"
        )
        return vendorMap[oui] ?: "Scheda Madre Generica ($oui)"
    }

    private fun detectBandwidth(scan: ScanResult): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return when (scan.channelWidth) {
                0 -> "20 MHz (Standard)"
                1 -> "40 MHz (Medio)"
                2 -> "80 MHz (Alta Velocità ⚡)"
                3 -> "160 MHz (Fibra Pura 🚀)"
                else -> "20 MHz"
            }
        }
        return "20 MHz"
    }

    private fun calculateUptime(timestampUs: Long): String {
        if (timestampUs <= 0) return "Sconosciuto"
        val totalSeconds = timestampUs / 1_000_000
        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            days > 0 -> "$days giorni, $hours ore"
            hours > 0 -> "$hours ore, $minutes min"
            else -> "$minutes minuti"
        }
    }

    private fun calculateDistance(levelDbm: Int, frequencyMhz: Int): Double {
        val exp = (27.55 - (20 * Math.log10(frequencyMhz.toDouble())) + Math.abs(levelDbm)) / 20.0
        return 10.0.pow(exp)
    }

    private fun convertFrequencyToChannel(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2412..2472 -> (freq - 2412) / 5 + 1
            freq in 5180..5825 -> (freq - 5180) / 5 + 36
            else -> freq
        }
    }

    private fun detectWifiTechnology(scan: ScanResult): String {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                return when (scan.wifiStandard) {
                    6 -> "Wi-Fi 6"
                    5 -> "Wi-Fi 5"
                    4 -> "Wi-Fi 4"
                    7 -> "Wi-Fi 7 ultra-veloce"
                    else -> "Standard Legacy"
                }
            } catch (e: Exception) {}
        }
        val cap = scan.capabilities ?: ""
        return when {
            cap.contains("HEW", ignoreCase = true) || cap.contains("802.11ax", ignoreCase = true) -> "Wi-Fi 6"
            cap.contains("VHT", ignoreCase = true) || cap.contains("802.11ac", ignoreCase = true) -> "Wi-Fi 5"
            cap.contains("HT", ignoreCase = true) || cap.contains("802.11n", ignoreCase = true) -> "Wi-Fi 4"
            else -> "Standard Legacy"
        }
    }
}

