package com.utsman.nadia.core

import com.github.ajalt.clikt.core.CliktError
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.utsman.nadia.data.KeystoreConfig
import com.utsman.nadia.data.KeystoreConfigString
import com.utsman.nadia.home
import com.utsman.nadia.util.Commander
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

class Installer {

    private val configNameFile = "nadia_keystore_config.json"
    private val configFile
        get() = File(home, configNameFile)
    private val gson
        get() = GsonBuilder()
        .setPrettyPrinting()
        .create()
    private val type
        get() = object : TypeToken<List<KeystoreConfigString>>() {}.type

    private fun getPackageName(bundletool: File, aab: File, adb: File): String {
        val commandPackageName = listOf(
            "java",
            "-jar",
            bundletool.absolutePath,
            "dump",
            "manifest",
            "--bundle=${aab.absolutePath}",
            "--xpath=/manifest/@package",
            "--adb=${adb.absolutePath}"
        )

        return Commander.execString(*commandPackageName.toTypedArray()).filter { !it.isWhitespace() }
    }

    fun createApks(bundletool: File, aab: File, adbFile: File, keystoreConfig: KeystoreConfig): File {
        val aabPath = aab.absolutePath
        val apksPath = aabPath.removeSuffix(".aab") + ".apks"
        val keystorePath = keystoreConfig.keystore.absolutePath
        val keystorePassword = keystoreConfig.keystorePass
        val keystoreAlias = keystoreConfig.keystoreAlias
        val keystoreAliasPassword = keystoreConfig.keystoreAliasPassword
        val adbPath = adbFile.absolutePath

        val existingApks = File(apksPath)
        if (existingApks.exists()) {
            existingApks.delete()
        }

        val command = listOf(
            "java",
            "-jar",
            bundletool.absolutePath,
            "build-apks",
            "--local-testing",
            "--connected-device",
            "--bundle=$aabPath",
            "--output=$apksPath",
            "--ks=$keystorePath",
            "--ks-pass=pass:$keystorePassword",
            "--ks-key-alias=$keystoreAlias",
            "--key-pass=pass:$keystoreAliasPassword",
            "--adb=$adbPath"
        )

        println("> Generate apks for ${aab.name}, please wait ..")
        Commander.exec(*command.toTypedArray())

        return File(apksPath)
    }

    fun installApks(bundletool: File, apks: File, adb: File) {
        val command = listOf(
            "java",
            "-jar",
            bundletool.absolutePath,
            "install-apks",
            "--apks=${apks.absolutePath}",
            "--adb=${adb.absolutePath}"
        )

        println("> Installing ${apks.name}, please wait ..")
        Commander.exec(*command.toTypedArray())
    }

    fun saveKeystoreConfig(aab: File, keystoreConfig: KeystoreConfig) {
        val saveToFile: (List<KeystoreConfigString>) -> Unit = { body ->
            try {
                PrintWriter(FileWriter(configFile.absolutePath)).use {
                    val json = gson.toJson(body, type)
                    it.write(json)
                }
            } catch (e: Exception) {
                throw CliktError(e.message)
            }
        }

        if (!configFile.exists()) {
            println("> Saving keystore configuration for ${aab.name} ..")
            val jsonData = listOf(keystoreConfig.toConfigString(aab))
            saveToFile.invoke(jsonData)
        } else {
            println("> Updating keystore configuration for ${aab.name} ..")
            val jsonString = configFile.readText()
            var existingConfigs = gson.fromJson<List<KeystoreConfigString>>(jsonString, type)
            val currentConfig = existingConfigs.find {
                it.aabFilePath == aab.absolutePath && it.keystorePath == keystoreConfig.keystore.absolutePath

            }
            if (currentConfig == null) {
                existingConfigs = existingConfigs + keystoreConfig.toConfigString(aab)
                saveToFile.invoke(existingConfigs)
            }
        }
    }

    fun getKeystoreConfig(aab: File, keystoreConfig: KeystoreConfig, onPrompt: (KeystoreConfig) -> Unit): Pair<KeystoreConfig, Boolean> {
        val newKeystoreConfig = if (!configFile.exists()) {
            println("> Launch prompter ..")
            Pair(keystoreConfig.apply(onPrompt), false)
        } else {
            println("> Use existing configuration keystore ..")
            val jsonString = configFile.readText()
            val existingConfigs = gson.fromJson<List<KeystoreConfigString>>(jsonString, type)
            val currentConfig = existingConfigs?.find {
                it.aabFilePath == aab.absolutePath && it.keystorePath == keystoreConfig.keystore.absolutePath
            }
            if (currentConfig == null) {
                println("> Keystore configuration not found in config file, launch prompter ..")
                val newKeystoreConfig = keystoreConfig.apply(onPrompt)
                Pair(newKeystoreConfig, false)
            } else {
                val newKeystoreConfig = keystoreConfig.apply {
                    keystorePass = currentConfig.keystorePass
                    keystoreAlias = currentConfig.keystoreAlias
                    keystoreAliasPassword = currentConfig.keystoreAliasPassword
                }
                Pair(newKeystoreConfig, true)
            }
        }

        return newKeystoreConfig
    }

    fun launchApp(bundletool: File, aab: File, adb: File) {
        val packageName = getPackageName(bundletool, aab, adb)

        val commandLauncher = listOf(
            adb.absolutePath,
            "shell",
            "cmd",
            "package",
            "resolve-activity",
            "--brief",
            "-c",
            "android.intent.category.LAUNCHER",
            packageName
        )


        val launcherResult = Commander.execString(*commandLauncher.toTypedArray())
            .split("\n")
            .filter { it.isNotEmpty() }

        val launcher = launcherResult.last()

        val command = listOf(
            adb.absolutePath,
            "shell",
            "am",
            "start",
            "-n",
            launcher.filter { !it.isWhitespace() }
        )

        println("> Found launcher: $launcher")
        println("> Launch app: $packageName")
        Commander.execInherit(*command.toTypedArray())
        println("> Waiting 10 second for launch app ..")
        Thread.sleep(10000)
        println("> Start log")

        val commandPid = listOf(
            adb.absolutePath,
            "shell",
            "pidof",
            "-s",
            packageName
        )

        val pidResult = Commander.execString(*commandPid.toTypedArray()).filter { !it.isWhitespace() }
        val commandLog = listOf(
            adb.absolutePath,
            "logcat",
            "--pid=$pidResult"
        )

        Commander.execInherit(*commandLog.toTypedArray())
    }

    fun uninstallApp(bundletool: File, aab: File, adb: File) {
        val packageName = getPackageName(bundletool, aab, adb)
        val command = listOf(
            adb.absolutePath,
            "uninstall",
            packageName
        )

        println("> Replace enable, uninstalling $packageName ...")
        Commander.execInherit(*command.toTypedArray())
    }
}