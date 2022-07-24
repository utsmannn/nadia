package com.utsman.nadia.data

import java.io.File

data class KeystoreConfig(
    var keystore: File,
    var keystorePass: String = "",
    var keystoreAlias: String = "",
    var keystoreAliasPassword: String = ""
) {
    fun toConfigString(aab: File): KeystoreConfigString {
        return KeystoreConfigString(
            aab.absolutePath, keystore.absolutePath, keystorePass, keystoreAlias, keystoreAliasPassword
        )
    }
}