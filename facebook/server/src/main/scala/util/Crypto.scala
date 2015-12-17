import java.security._
import java.security.spec._
import javax.crypto._
import javax.crypto.spec._

object Crypto {
    object rsa {
        def generateKeyPair: KeyPair = {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(1024)
            kpg.genKeyPair()
        }

        def encodePublicKey(key: PublicKey) = {
            new sun.misc.BASE64Encoder().encode(key.getEncoded())
        }

        def encodePrivateKey(key: PrivateKey) = {
            new sun.misc.BASE64Encoder().encode(key.getEncoded())
        }

        def decodePublicKey(encryptedKey: String) = {
            val spec = new X509EncodedKeySpec(new sun.misc.BASE64Decoder().decodeBuffer(encryptedKey))
            val factory = KeyFactory.getInstance("RSA")
            factory.generatePublic(spec)
        }

        def decodePrivateKey(encryptedKey: String) = {
            val spec = new PKCS8EncodedKeySpec(new sun.misc.BASE64Decoder().decodeBuffer(encryptedKey))
            val factory = KeyFactory.getInstance("RSA")
            factory.generatePrivate(spec)
        }

        def encrypt(message: String, key: PublicKey): String = {
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            new sun.misc.BASE64Encoder().encode(cipher.doFinal(message.getBytes()))
        }

        def encrypt(message: String, key: PrivateKey): String = {
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            new sun.misc.BASE64Encoder().encode(cipher.doFinal(message.getBytes()))
        }

        def decrypt(encryptedMessage: String, key: PublicKey): String = {
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val stringBytes = cipher.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(encryptedMessage))
            new String(stringBytes)
        }

        def decrypt(encryptedMessage: String, key: PrivateKey): String = {
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val stringBytes = cipher.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(encryptedMessage))
            new String(stringBytes)
        }
    }

    object aes {
        def encrypt(message: String, key: SecretKey, initVector: String): String = {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            // val iv = new IvParameterSpec(new sun.misc.BASE64Decoder().decodeBuffer(initVector))
            val iv = new IvParameterSpec(initVector.getBytes)
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)
            new sun.misc.BASE64Encoder().encode(cipher.doFinal(message.getBytes("UTF-8")))
        }

        def decrypt(encryptedMessage: String, key: SecretKey, initVector: String): String = {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = new IvParameterSpec(initVector.getBytes)
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val stringBytes = cipher.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(encryptedMessage))
            new String(stringBytes)
        }

        def generateSecretKey : SecretKey = {
            val kg = KeyGenerator.getInstance("AES")
            kg.init(128)
            kg.generateKey
        }

        def encodeKey(key: SecretKey) = {
            new sun.misc.BASE64Encoder().encode(key.getEncoded())
        }

        def decodeKey(encryptedKey: String) = {
            new SecretKeySpec(new sun.misc.BASE64Decoder().decodeBuffer(encryptedKey), "AES")
        }
    }

    val srng = new SecureRandom()
}

object CryptoTest {
    def testRSA = {
        // RSA
        val kp = Crypto.rsa.generateKeyPair
        println(kp.getPublic())
        println("--------")
        println(Crypto.rsa.encodePublicKey(kp.getPublic()))
        println("--------")
        println(Crypto.rsa.decodePublicKey(Crypto.rsa.encodePublicKey(kp.getPublic())))
        println("--------")
        println(Crypto.rsa.encodePublicKey(Crypto.rsa.decodePublicKey(Crypto.rsa.encodePublicKey(kp.getPublic()))))
        println("--------")
        println("--------")
        println(kp.getPrivate())
        println("--------")
        println(Crypto.rsa.encodePrivateKey(kp.getPrivate()))
        println("--------")
        println(Crypto.rsa.decodePrivateKey(Crypto.rsa.encodePrivateKey(kp.getPrivate())))
        println("--------")
        println(Crypto.rsa.encodePrivateKey(Crypto.rsa.decodePrivateKey(Crypto.rsa.encodePrivateKey(kp.getPrivate()))))
        println("--------")


        val pub = kp.getPublic()
        val priv = kp.getPrivate()

        val m1 = "This message should be encrypted correctly"
        println(m1)
        println("--------")

        val em1 = Crypto.rsa.encrypt(m1,pub)
        println(em1)
        println("--------")

        val dm1 = Crypto.rsa.decrypt(em1,priv)
        println(dm1)
        println("--------")

        if(dm1 == m1) {
            println("Step 1 successful")
        } else {
            println("Error: Step 1 failed")
        }
        println("--------")

        val m2 = "This message should be digitally signed correctly"
        println(m2)
        println("--------")

        val em2 = Crypto.rsa.encrypt(m2,priv)
        println(em2)
        println("--------")

        val dm2 = Crypto.rsa.decrypt(em2,pub)
        println(dm2)
        println("--------")

        if(dm2 == m2) {
            println("step 2 successful")
        } else {
            println("Error: Step 2 failed")
        }

        println("--------")
        println("--------")
        println("Failure cases")
        println("--------")
        println("--------")

        val m3 = "testing both encryption and decryption with public key only"
        println(m3)
        println("--------")

        val em3 = Crypto.rsa.encrypt(m3,pub)
        println(em3)
        println("--------")

        try { 
            val dm3 = Crypto.rsa.decrypt(em3,pub)
            println(dm3)

            if(dm3 == m3) {
                println("step 3 successful")
            } else {
                println("Error: Step 3 failed")
            }
        } catch {
          case e: Exception => println(e.getMessage)
        }
        println("--------")

        val m4 = "testing both encryption and decryption with private key only"
        println(m4)
        println("--------")

        val em4 = Crypto.rsa.encrypt(m4,priv)
        println(em4)
        println("--------")

        try { 
            val dm4 = Crypto.rsa.decrypt(em4,priv)
            println(dm4)

            if(dm4 == m4) {
                println("step 4 successful")
            } else {
                println("Error: Step 4 failed")
            }
        } catch {
          case e: Exception => println(e.getMessage)
        }
        println("--------")
    }

    def testAES = {
        // AES
        val k = Crypto.aes.generateSecretKey

        println(k)
        println("--------")
        println(Crypto.aes.encodeKey(k));
        println("--------")
        println(Crypto.aes.decodeKey(Crypto.aes.encodeKey(k)));
        println("--------")
        println(Crypto.aes.encodeKey(Crypto.aes.decodeKey(Crypto.aes.encodeKey(k))));
        println("--------")

       /* val message = "Hello Crypto world!!"
        println(message)
        println("--------")

        val iv = new IvParameterSpec(Crypto.srng.generateSeed(16))
        println(iv)
        println("--------")

        val encryptedMessage = Crypto.aes.encrypt(message,k,iv)
        println(encryptedMessage)
        println("--------")
        println(Crypto.aes.decrypt(encryptedMessage,k,iv))
        println("--------")*/
    }
}