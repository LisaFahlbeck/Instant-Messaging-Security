import java.security.SecureRandom;

public class Curve25519 {
	
	final static int KEY_LENGTH = 32;
	final static int NUMBER_OF_EPHEMERAL_KEYS = 10;
	final static Sha512 sha512provider = new sha512_provider();
	
	
	/**
	 * 
	 * @param length: Length of the desired byte-array
	 * @return byte-array with random bytes
	 */
	public byte[] getRandom(int length) {
		SecureRandom random = new SecureRandom();
		byte[] result = new byte[length];
		random.nextBytes(result);
		return result;
	}
	/**
	 * 
	 * @param random: Array of random bytes
	 * @return "Curve-friendly" byte-array
	 */
	
	public byte[] generatePrivateKey(byte[] random) {
		byte[] privateKey = new byte[KEY_LENGTH];
		
		System.arraycopy(random, 0, privateKey, 0, 32);
		
	    privateKey[0]  &= 248;
	    privateKey[31] &= 127;
	    privateKey[31] |= 64;

	    return privateKey;
	}
	/**
	 * 
	 * @param privateKey: "Curve-friendly" byte array (with length 32)
	 * @return PublicKey that can be used in key-exchanges
	 */
	public byte[] generatePublicKey(byte[] privateKey) {
		byte[] publicKey = new byte[KEY_LENGTH];
		curve_sigs.curve25519_keygen(publicKey, privateKey);
		return publicKey;
	}
	/**
	 * 
	 * @return A Curve25519 key-pair
	 */
	public Curve_KeyPair generateKeyPair() {
		byte[] privateKey = generatePrivateKey(getRandom(KEY_LENGTH));
		byte[] publicKey = generatePublicKey(privateKey);
		return new Curve_KeyPair(privateKey, publicKey);
	}
	/**
	 * 
	 * @return An array of Curve25519 key-pairs, where the length depends on global variable
	 */
	
	public Curve_KeyPair[] generatePreKeys() {
		Curve_KeyPair[] preKeys= new Curve_KeyPair[NUMBER_OF_EPHEMERAL_KEYS];
		for(int i = 0; i < preKeys.length; i++) {
			preKeys[i] = generateKeyPair(); 
		}
		return preKeys;
	}
	/**
	 * 
	 * @return A pre-key-bundle, containing both the private and the public parts of the keys
	 */
	
	public preKeyBundle generatePreKeyBundle() {
		Curve_KeyPair identityKey = generateKeyPair();
		Curve_KeyPair preKey = generateKeyPair();
		Curve_KeyPair[] preKeys = generatePreKeys();
		
		byte[] signedPublicPreKey = new byte[64];
		curve_sigs.curve25519_sign(sha512provider, signedPublicPreKey, identityKey.getPublicKey(), preKey.getPublicKey(), preKey.getPublicKey().length, getRandom(32));
		
		byte[][] privatePreKeys = new byte[NUMBER_OF_EPHEMERAL_KEYS][KEY_LENGTH];
		byte[][] publicPreKeys = new byte[NUMBER_OF_EPHEMERAL_KEYS][KEY_LENGTH];
		for(int i = 0; i < NUMBER_OF_EPHEMERAL_KEYS; i++) {
			privatePreKeys[i] = preKeys[i].getPrivateKey();
			publicPreKeys[i] = preKeys[i].getPublicKey();
		}
		
		preKeyBundlePrivate privateKeys = new preKeyBundlePrivate(identityKey.getPrivateKey(), preKey.getPrivateKey(), privatePreKeys);
		preKeyBundlePublic publicKeys = new preKeyBundlePublic(identityKey.getPublicKey(), signedPublicPreKey, preKey.getPublicKey(), publicPreKeys);
		return new preKeyBundle(privateKeys, publicKeys);
	}
	/**
	 * 
	 * @param ourPrivate: Our privateKey
	 * @param theirPublic: Their publicKey
	 * @return a shared agreement 
	 */
	public byte[] calculateAgreement(byte[] ourPrivate, byte[] theirPublic) {
	    byte[] agreement = new byte[32];
	    scalarmult.crypto_scalarmult(agreement, ourPrivate, theirPublic);

	    return agreement;
	 }
}
