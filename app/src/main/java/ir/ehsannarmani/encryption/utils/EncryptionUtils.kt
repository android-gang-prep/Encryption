package ir.ehsannarmani.encryption.utils

import android.R.attr.password
import android.R.attr.path
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec


object EncryptionUtils {
    const val SALT = "abcd"
    fun encrypt(path:String,password:String):File{
        val fis = FileInputStream(path)
        val fos = FileOutputStream("$path.crypt")
        var key = (SALT + password).toByteArray()
        val sha = MessageDigest.getInstance("SHA-1")
        key = sha.digest(key)
        key = key.copyOf(16)
        val sks = SecretKeySpec(key,"AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE,sks)
        val cos = CipherOutputStream(fos,cipher)
        var b:Int
        val d = ByteArray(8)
        while(fis.read(d).also { b = it } != -1) {
            cos.write(d, 0, b);
        }
        cos.flush()
        cos.close()
        fis.close()

        return File("$path.crypt")
    }
    fun decrypt(
        path: String,
        outputPath:String,
        password: String
    ){
        val fis = FileInputStream(path)
        val fos = FileOutputStream(outputPath)
        var key: ByteArray = (SALT + password).toByteArray()
        val sha = MessageDigest.getInstance("SHA-1")
        key = sha.digest(key)
        key = key.copyOf(16)
        val sks = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, sks)
        val cis = CipherInputStream(fis, cipher)
        var b: Int
        val d = ByteArray(8)
        while (cis.read(d).also { b = it } != -1) {
            fos.write(d, 0, b)
        }
        fos.flush()
        fos.close()
        cis.close()
    }
}