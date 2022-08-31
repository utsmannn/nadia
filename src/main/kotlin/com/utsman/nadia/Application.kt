package com.utsman.nadia

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.utsman.nadia.core.Downloader
import com.utsman.nadia.core.Installer
import com.utsman.nadia.data.KeystoreConfig
import java.io.File
import kotlin.system.exitProcess

val home: String get() = System.getProperty("user.home") + File.separator + "nadia"

private const val version = "v1.0.20"

class Application : CliktCommand(help = """
    ```
    Nadia
    Android App Bundle Installer (aab installer)
    $version
    ```
""".trimIndent()) {
    private val aab by option("-a", "--aab", help = "Bundle aab file").file().required().validate {
        require(it.extension == "aab") {
            "Aab invalid!"
        }
    }

    private val keyStore by option("-k", "--keystore", help = "Keystore of aab file").file().required().validate {
        require(it.extension == "keystore" || it.extension == "jks" ) {
            "Keystore invalid!"
        }
    }

    private val replace by option("-r", "--replace", help = "Enable replace, the application will be uninstall " +
            "first before install new version of aab").flag(default = false)

    private val downloader = Downloader()
    private val installer = Installer()

    override fun run() {
        println("""
          
          --------------------------------------------
            Nadia
            Android App Bundle Installer (aab installer)
            $version
          --------------------------------------------
          
        """.trimIndent())
        downloader.getBundletool { bundletool ->
            downloader.getAdbPlatformTool { adb ->
                val (keystoreConfig, isExists) = installer.getKeystoreConfig(aab, KeystoreConfig(keystore = keyStore)) {
                    println("> Setup config for $aab:")
                    it.keystorePass = prompt("Keystore password").orEmpty()
                    it.keystoreAlias = prompt("Keystore alias").orEmpty()
                    it.keystoreAliasPassword = prompt("Keystore password for alias").orEmpty()
                }

                val apks = installer.createApks(bundletool, aab, adb, keystoreConfig)
                if (!isExists) {
                    installer.saveKeystoreConfig(aab, keystoreConfig)
                }
                if (replace) {
                    installer.uninstallApp(bundletool, aab, adb)
                }
                installer.installApks(bundletool, apks, adb)
                installer.launchApp(bundletool, aab, adb)
                exitProcess(1)
            }
        }
    }
}

fun main(args: Array<String>) = Application().main(args)
