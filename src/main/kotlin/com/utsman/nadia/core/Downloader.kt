package com.utsman.nadia.core

import com.github.ajalt.clikt.core.CliktError
import com.utsman.nadia.home
import net.lingala.zip4j.ZipFile
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.RoundingMode

class Downloader {

    enum class OS {
        WINDOWS, LINUX, MAC, SOLARIS
    }

    private val client = OkHttpClient()

    private val adbName
        get() = if (getOS() == OS.WINDOWS) {
            "adb.exe"
        } else {
            "adb"
        }

    interface Listener {
        fun onDownloading(downloaded: Long, size: Long)
        fun onSuccess(file: File)
        fun onFailure(throwable: Throwable)
    }

    private fun Double.scaleRound(): Double {
        return toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
    }

    private fun Double.toReadable(): String {
        return if (this >= 1024000) {
            val rawResult = this / 1024000
            val result = rawResult.scaleRound()
            "${result}MB"
        } else {
            "${this}KB"
        }
    }

    private fun getOS(): OS {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> {
                OS.WINDOWS
            }
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> {
                OS.LINUX
            }
            os.contains("mac") -> {
                OS.MAC
            }
            os.contains("sunos") -> {
                OS.SOLARIS
            }
            else -> throw CliktError("Unknown operating system!")
        }
    }

    fun getBundletool(bundletool: (File) -> Unit) {
        val filename = "bundletool.jar"

        val fileDirectory = File(home, "bundletool")
        val fileTarget = File(fileDirectory, filename)

        if (!fileDirectory.exists()) {
            fileDirectory.mkdirs()
        }

        val bundletoolUrl = "https://github.com/google/bundletool/releases/download/1.11.0/bundletool-all-1.11.0.jar"
        if (fileTarget.exists()) {
            bundletool.invoke(fileTarget)
        } else {
            download(bundletoolUrl, fileTarget, object : Listener {
                override fun onDownloading(downloaded: Long, size: Long) {
                    // ..
                }

                override fun onSuccess(file: File) {
                    println("> Download bundletool success!")
                    bundletool.invoke(file)
                }

                override fun onFailure(throwable: Throwable) {
                    println("> Failure: ${throwable.message}")
                }
            })
        }
    }

    fun getAdbPlatformTool(adb: (File) -> Unit) {
        val platformToolUrl = when (getOS()) {
            OS.WINDOWS -> "https://dl.google.com/android/repository/platform-tools-latest-windows.zip?hl=id"
            OS.MAC -> "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip?hl=id"
            else -> "https://dl.google.com/android/repository/platform-tools-latest-linux.zip?hl=id"
        }

        val filenameDownloaded = "platform-tools.zip"
        val fileDirectory = File(home, "platform-tools")
        if (fileDirectory.exists()) {
            val fileAdb = File(fileDirectory, adbName)
            if (fileAdb.exists()) {
                adb.invoke(fileAdb)
            }
        } else {
            val fileTargetDownloaded = File(home, filenameDownloaded)

            download(platformToolUrl, fileTargetDownloaded, object : Listener {
                override fun onDownloading(downloaded: Long, size: Long) {
                    // ..
                }

                override fun onSuccess(file: File) {
                    println("> Download android platform-tool success!")
                    unzip(file, fileDirectory) { resultDirectory ->

                        val fileAdb = File(resultDirectory, adbName)
                        if (fileAdb.exists()) {
                            fileTargetDownloaded.delete()
                            adb.invoke(fileAdb)
                        } else {
                            throw CliktError("Failure: adb not found!")
                        }
                    }
                }

                override fun onFailure(throwable: Throwable) {
                    println("> Failure: ${throwable.message}")
                }
            })
        }
    }

    private fun unzip(zipFile: File, destinationDirectory: File, resultDirectory: (File) -> Unit) {
        ZipFile(zipFile).extractAll(destinationDirectory.parentFile.absolutePath)
        resultDirectory.invoke(destinationDirectory)
    }

    private fun download(url: String, fileTarget: File, listener: Listener) {
        println("> Start download ${fileTarget.name}...")
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    e.printStackTrace()
                    listener.onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val inputStream = response.body?.byteStream()
                    val outputStream = FileOutputStream(fileTarget)

                    val buff = ByteArray(1024 * 4)
                    var downloaded: Long = 0
                    val target = response.body?.contentLength()

                    if (target != null && target > 0L) {
                        listener.onDownloading(0, target)
                        while (downloaded != target) {
                            val readed = inputStream?.read(buff)
                            if (readed == -1) {
                                break
                            }
                            if (readed != null) {
                                print("""
                                    Downloading ${fileTarget.name}: ${downloaded.toDouble().toReadable()} of ${target.toDouble().toReadable()}
                                """.trimIndent() + " ".repeat(50) + "\r")
                                listener.onDownloading(downloaded, target)
                                outputStream.write(buff, 0, readed)
                                downloaded += readed.toLong()
                            }
                        }
                    }

                    outputStream.flush()
                    outputStream.close()

                    listener.onSuccess(fileTarget)
                }
            })
        } catch (e: Throwable) {
            listener.onFailure(e)
        } catch (e: IOException) {
            listener.onFailure(e)
        }
    }
}