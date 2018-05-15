package org.brewchain.ecrypto.address.iota;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class JCurl implements ICurl {

    /**
     * The hash length.
     */
    public static final int HASH_LENGTH = 243;
    private static final int STATE_LENGTH = 3 * HASH_LENGTH;

    public static final int NUMBER_OF_ROUNDSP81 = 81;
    public static final int NUMBER_OF_ROUNDSP27 = 27;
    private final int numberOfRounds;

    private static final int[] TRUTH_TABLE = {1, 0, -1, 2, 1, -1, 0, 2, -1, 1, 0};
    private final long[] stateLow;
    private final long[] stateHigh;
    private final int[] scratchpad = new int[STATE_LENGTH];
    private int[] state;

    public JCurl(boolean pair, SpongeFactory.Mode mode) {
        switch (mode) {
            case CURLP27: {
                numberOfRounds = NUMBER_OF_ROUNDSP27;
            }
            break;
            case CURLP81: {
                numberOfRounds = NUMBER_OF_ROUNDSP81;
            }
            break;
            default:
                throw new NoSuchElementException("Only Curl-P-27 and Curl-P-81 are supported.");
        }
        if (pair) {
            stateHigh = new long[STATE_LENGTH];
            stateLow = new long[STATE_LENGTH];
            state = null;
            set();
        } else {
            state = new int[STATE_LENGTH];
            stateHigh = null;
            stateLow = null;
        }
    }

    public JCurl(SpongeFactory.Mode mode) {
        switch (mode) {
            case CURLP27: {
                numberOfRounds = NUMBER_OF_ROUNDSP27;
            }
            break;
            case CURLP81: {
                numberOfRounds = NUMBER_OF_ROUNDSP81;
            }
            break;
            default:
                throw new NoSuchElementException("Only Curl-P-27 and Curl-P-81 are supported.");
        }
        state = new int[STATE_LENGTH];
        stateHigh = null;
        stateLow = null;
    }

    /**
     * Absorbs the specified trits.
     *
     * @param trits  The trits.
     * @param offset The offset to start from.
     * @param length The length.
     * @return The ICurl instance (used for method chaining).
     */
    public JCurl absorb(final int[] trits, int offset, int length) {

        do {
            System.arraycopy(trits, offset, state, 0, length < HASH_LENGTH ? length : HASH_LENGTH);
            transform();
            offset += HASH_LENGTH;
        } while ((length -= HASH_LENGTH) > 0);

        return this;
    }

    /**
     * Absorbs the specified trits.
     *
     * @param trits The trits.
     * @return The ICurl instance (used for method chaining).
     */
    public JCurl absorb(final int[] trits) {
        return absorb(trits, 0, trits.length);
    }

    /**
     * Transforms this instance.
     *
     * @return The ICurl instance (used for method chaining).
     */
    public JCurl transform() {

        int scratchpadIndex = 0;
        int prev_scratchpadIndex = 0;
        for (int round = 0; round < numberOfRounds; round++) {
            System.arraycopy(state, 0, scratchpad, 0, STATE_LENGTH);
            for (int stateIndex = 0; stateIndex < STATE_LENGTH; stateIndex++) {
                prev_scratchpadIndex = scratchpadIndex;
                if (scratchpadIndex < 365) {
                    scratchpadIndex += 364;
                } else {
                    scratchpadIndex += -365;
                }
                state[stateIndex] = TRUTH_TABLE[scratchpad[prev_scratchpadIndex] + (scratchpad[scratchpadIndex] << 2) + 5];
            }
        }

        return this;
    }

    /**
     * Resets this state.
     *
     * @return The ICurl instance (used for method chaining).
     */
    public JCurl reset() {
        Arrays.fill(state, 0);
        return this;
    }

    public JCurl reset(boolean pair) {
        if (pair) {
            set();
        } else {
            reset();
        }
        return this;
    }

    /**
     * Squeezes the specified trits.
     *
     * @param trits  The trits.
     * @param offset The offset to start from.
     * @param length The length.
     * @return The squeezes trits.
     */
    public int[] squeeze(final int[] trits, int offset, int length) {

        do {
            System.arraycopy(state, 0, trits, offset, length < HASH_LENGTH ? length : HASH_LENGTH);
            transform();
            offset += HASH_LENGTH;
        } while ((length -= HASH_LENGTH) > 0);

        return state;
    }

    /**
     * Squeezes the specified trits.
     *
     * @param trits The trits.
     * @return The squeezes trits.
     */
    public int[] squeeze(final int[] trits) {
        return squeeze(trits, 0, trits.length);
    }

    /**
     * Gets the states.
     *
     * @return The state.
     */
    public int[] getState() {
        return state;
    }

    /**
     * Sets the state.
     *
     * @param state The states.
     */
    public void setState(int[] state) {
        this.state = state;
    }

    private void set() {
        Arrays.fill(stateLow, Converter.HIGH_LONG_BITS);
        Arrays.fill(stateHigh, Converter.HIGH_LONG_BITS);
    }

    private void pairTransform() {
        final long[] curlScratchpadLow = new long[STATE_LENGTH];
        final long[] curlScratchpadHigh = new long[STATE_LENGTH];
        int curlScratchpadIndex = 0;
        for (int round = numberOfRounds; round-- > 0; ) {
            System.arraycopy(stateLow, 0, curlScratchpadLow, 0, STATE_LENGTH);
            System.arraycopy(stateHigh, 0, curlScratchpadHigh, 0, STATE_LENGTH);
            for (int curlStateIndex = 0; curlStateIndex < STATE_LENGTH; curlStateIndex++) {
                final long alpha = curlScratchpadLow[curlScratchpadIndex];
                final long beta = curlScratchpadHigh[curlScratchpadIndex];
                final long gamma = curlScratchpadHigh[curlScratchpadIndex += (curlScratchpadIndex < 365 ? 364 : -365)];
                final long delta = (alpha | (~gamma)) & (curlScratchpadLow[curlScratchpadIndex] ^ beta);
                stateLow[curlStateIndex] = ~delta;
                stateHigh[curlStateIndex] = (alpha ^ gamma) | delta;
            }
        }
    }


    public void absorb(final Pair<long[], long[]> pair, int offset, int length) {
        int o = offset, l = length, i = 0;
        do {
            System.arraycopy(pair.low, o, stateLow, 0, l < HASH_LENGTH ? l : HASH_LENGTH);
            System.arraycopy(pair.hi, o, stateHigh, 0, l < HASH_LENGTH ? l : HASH_LENGTH);
            pairTransform();
            o += HASH_LENGTH;
        } while ((l -= HASH_LENGTH) > 0);
    }

    public Pair<long[], long[]> squeeze(Pair<long[], long[]> pair, int offset, int length) {
        int o = offset, l = length, i = 0;
        long[] low = pair.low;
        long[] hi = pair.hi;
        do {
            System.arraycopy(stateLow, 0, low, o, l < HASH_LENGTH ? l : HASH_LENGTH);
            System.arraycopy(stateHigh, 0, hi, o, l < HASH_LENGTH ? l : HASH_LENGTH);
            pairTransform();
            o += HASH_LENGTH;
        } while ((l -= HASH_LENGTH) > 0);
        return new Pair<>(low, hi);
    }

    /**
     * Clones this instance.
     *
     * @return A new instance.
     */
    @Override
    public ICurl clone() {
        return new JCurl(SpongeFactory.Mode.CURLP81);
    }
}
