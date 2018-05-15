package org.brewchain.ecrypto.address.iota;

public class Pair<S, T> {
    public S low;
    public T hi;

    public Pair(S k, T v) {
        low = k;
        hi = v;
    }
}