package org.brewchain.ecrypto.address.iota;

public class Signing {
    public final static int KEY_LENGTH = 6561;

    //private ICurl curl;

    /**
     * public Signing() {
     * this(null);
     * }
     *
     * /**
     *
     * @param curl
     */
    
    public Signing() {
    		
    }
    
    public Signing(ICurl curl) {
        //this.curl = curl == null ? SpongeFactory.create(SpongeFactory.Mode.KERL) : curl;
    }

    /**
     * @param inSeed
     * @param index
     * @param security
     * @return
     * @throws ArgumentException is thrown when the specified security level is not valid.
     */
    public int[] key(final int[] inSeed, final int index, int security) throws Exception {
        if (security < 1) {
            throw new Exception(Constants.INVALID_SECURITY_LEVEL_INPUT_ERROR);
        }

        int[] seed = inSeed.clone();

        // Derive subseed.
        for (int i = 0; i < index; i++) {
            for (int j = 0; j < seed.length; j++) {
                if (++seed[j] > 1) {
                    seed[j] = -1;
                } else {
                    break;
                }
            }
        }

        ICurl curl = this.getICurlObject(SpongeFactory.Mode.KERL);
        curl.reset();
        curl.absorb(seed, 0, seed.length);
        // seed[0..JCurl.HASH_LENGTH] contains subseed
        curl.squeeze(seed, 0, seed.length);
        curl.reset();
        // absorb subseed
        curl.absorb(seed, 0, seed.length);

        final int[] key = new int[security * JCurl.HASH_LENGTH * 27];
        final int[] buffer = new int[seed.length];
        int offset = 0;

        while (security-- > 0) {
            for (int i = 0; i < 27; i++) {
                curl.squeeze(buffer, 0, seed.length);
                System.arraycopy(buffer, 0, key, offset, JCurl.HASH_LENGTH);

                offset += JCurl.HASH_LENGTH;
            }
        }
        return key;
    }
    
    public int[] address(int[] digests) {
        int[] address = new int[JCurl.HASH_LENGTH];
        ICurl curl = this.getICurlObject(SpongeFactory.Mode.KERL);
        curl.reset()
                .absorb(digests)
                .squeeze(address);
        return address;
    }

    public int[] digests(int[] key) {
        int security = (int) Math.floor(key.length / KEY_LENGTH);

        int[] digests = new int[security * JCurl.HASH_LENGTH];
        int[] keyFragment = new int[KEY_LENGTH];

        ICurl curl = this.getICurlObject(SpongeFactory.Mode.KERL);
        for (int i = 0; i < Math.floor(key.length / KEY_LENGTH); i++) {
            System.arraycopy(key, i * KEY_LENGTH, keyFragment, 0, KEY_LENGTH);

            for (int j = 0; j < 27; j++) {
                for (int k = 0; k < 26; k++) {
                    curl.reset()
                            .absorb(keyFragment, j * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH)
                            .squeeze(keyFragment, j * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
                }
            }

            curl.reset();
            curl.absorb(keyFragment, 0, keyFragment.length);
            curl.squeeze(digests, i * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
        }
        return digests;
    }
    
    private ICurl getICurlObject(SpongeFactory.Mode mode) {
    		return SpongeFactory.create(mode);
    }
}

