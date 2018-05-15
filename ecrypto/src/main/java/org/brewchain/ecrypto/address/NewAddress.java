package org.brewchain.ecrypto.address;

import java.security.SecureRandom;
import java.util.List;

import org.brewchain.core.crypto.ECKey;

public interface NewAddress {
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
    public List<String> newAddress(final String seed, int security, final int index, final boolean checksum, final int total, final boolean returnAll) throws Exception;
    
    
    public ECKey newAddress(SecureRandom ran) throws Exception;
    
}
