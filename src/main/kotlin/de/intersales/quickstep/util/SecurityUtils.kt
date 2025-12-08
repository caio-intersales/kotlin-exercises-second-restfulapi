package de.intersales.quickstep.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

// Als ich die pom.xml-Datei nicht ändern wollte, habe ich diese (nicht sichere) Funktion gemacht, nur um das Passwort hashen zu können, ohne neue Libraries hinzuzufügen

fun sha256Hash (rawPassword: String): String {
    val digest  = MessageDigest.getInstance("SHA-256")
    val hash    = digest.digest(rawPassword.toByteArray(StandardCharsets.UTF_8))

    // Convert the raw password and return the hashed one
    return hash.joinToString(""){
        "%02x".format(it)
    }
}