package org.brewchain.ecrypto.address;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.brewchain.core.crypto.ECKey;
import org.brewchain.ecrypto.address.iota.Constants;
import org.brewchain.ecrypto.address.iota.Converter;
import org.brewchain.ecrypto.address.iota.ICurl;
import org.brewchain.ecrypto.address.iota.JCurl;
import org.brewchain.ecrypto.address.iota.Signing;
import org.brewchain.ecrypto.address.iota.SpongeFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IoTANewAddress implements NewAddress {
	
	/**
     * Generates a new address from a seed and returns the remainderAddress.
     * This is either done deterministically, or by providing the index of the new remainderAddress.
     *
     * @param seed      Tryte-encoded seed. It should be noted that this seed is not transferred.
     * @param security  Security level to be used for the private key / address. Can be 1, 2 or 3.
     * @param index     Key index to start search from. If the index is provided, the generation of the address is not deterministic.
     * @param checksum  Adds 9-tryte address checksum.
     * @param total     Total number of addresses to generate.
     * @param returnAll If <code>true</code>, it returns all addresses which were deterministically generated (until findTransactions returns null).
     * @return An array of strings with the specifed number of addresses.
     * @throws ArgumentException is thrown when the specified input is not valid.
     */
    public List<String> newAddress(final String seed, int security, final int index, final boolean checksum, final int total, final boolean returnAll) throws Exception {
        
    		List<String> allAddresses = new ArrayList<>();

        // If total number of addresses to generate is supplied, simply generate
        // and return the list of all addresses
        // ICurl customCurl = SpongeFactory.create(SpongeFactory.Mode.KERL);
        if (total != 0) {
            for (int i = index; i < index + total; i++) {
                // allAddresses.add(newAddress(seed, security, i, checksum, customCurl.clone()));
            		allAddresses.add(newAddress(seed, security, i, checksum));
            }
        }

        // If !returnAll return only the last address that was generated
        if (!returnAll) {
            //allAddresses = allAddresses.subList(allAddresses.size() - 2, allAddresses.size() - 1);
            allAddresses = allAddresses.subList(allAddresses.size() - 1, allAddresses.size());
        }
        return allAddresses;
    }
    
    /**
     * Generates a new address
     *
     * @param seed     The tryte-encoded seed. It should be noted that this seed is not transferred.
     * @param security The secuirty level of private key / seed.
     * @param index    The index to start search from. If the index is provided, the generation of the address is not deterministic.
     * @param checksum The adds 9-tryte address checksum
     * @param curl     The curl instance.
     * @return An String with address.
     * @throws ArgumentException is thrown when the specified input is not valid.
     */
    private static String newAddress(String seed, int security, int index, boolean checksum, ICurl curl) throws Exception {

        if (security < 1) {
            throw new Exception(Constants.INVALID_SECURITY_LEVEL_INPUT_ERROR);
        }

        Signing signing = new Signing(curl);
        
        final int[] key = signing.key(Converter.trits(seed), index, security);
        
        final int[] digests = signing.digests(key);
        
        final int[] addressTrits = signing.address(digests);

        String address = Converter.trytes(addressTrits);

        if (checksum) {
            address = addChecksum(address);
        }
        return address;
    }
    /**
     * Generates a new address
     *
     * @param seed     The tryte-encoded seed. It should be noted that this seed is not transferred.
     * @param security The secuirty level of private key / seed.
     * @param index    The index to start search from. If the index is provided, the generation of the address is not deterministic.
     * @param checksum The adds 9-tryte address checksum
     * @return An String with address.
     * @throws Exception is thrown when the specified input is not valid.
     */
    private static String newAddress(String seed, int security, int index, boolean checksum) throws Exception {

        if (security < 1) {
            throw new Exception(Constants.INVALID_SECURITY_LEVEL_INPUT_ERROR);
        }

        Signing signing = new Signing();
        
        final int[] key = signing.key(Converter.trits(seed), index, security);
        
        final int[] digests = signing.digests(key);
        
        final int[] addressTrits = signing.address(digests);

        String address = Converter.trytes(addressTrits);

        if (checksum) {
            address = addChecksum(address);
        }
        return address;
    }
    
    /**
     * Adds the checksum to the specified address.
     *
     * @param address The address without checksum.
     * @return The address with the appended checksum.
     * @throws Exception is thrown when the specified address is not an valid address.
     **/
    private static String addChecksum(String address) throws Exception {
    		if (!isAddress(address)) {
            throw new Exception(Constants.INVALID_ADDRESSES_INPUT_ERROR);
        }
        String addressWithChecksum = address;
        addressWithChecksum += calculateChecksum(address);
        return addressWithChecksum;
    }
    private static String calculateChecksum(String address) {
        ICurl curl = SpongeFactory.create(SpongeFactory.Mode.KERL);
        curl.reset();
        curl.absorb(Converter.trits(address));
        int[] checksumTrits = new int[JCurl.HASH_LENGTH];
        curl.squeeze(checksumTrits);
        String checksum = Converter.trytes(checksumTrits);
        return checksum.substring(72, 81);
    }
   /**
    * Determines whether the specified string is an address.
    *
    * @param address The address to validate.
    * @return <code>true</code> if the specified string is an address; otherwise, <code>false</code>.
    **/
   public static boolean isAddress(String address) {
       return (address.length() == Constants.ADDRESS_LENGTH_WITHOUT_CHECKSUM ||
               address.length() == Constants.ADDRESS_LENGTH_WITH_CHECKSUM) && isTrytes(address, address.length());
   }
   /**
    * Determines whether the specified string contains only characters from the trytes alphabet (see <see cref="Constants.TryteAlphabet"/>).
    *
    * @param trytes The trytes to validate.
    * @param length The length.
    * @return <code>true</code> if the specified trytes are trytes otherwise, <code>false</code>.
    **/
   public static boolean isTrytes(final String trytes, final int length) {
       return trytes.matches("^[A-Z9]{" + (length == 0 ? "0," : length) + "}$");
   }

	@Override
	public ECKey newAddress(SecureRandom ran) throws Exception {
		// TODO Auto-generated method stub
		return null;
}
}
