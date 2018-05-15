package org.brewchain.ecrypto.address;

public abstract class AddressFactory {
    public static NewAddress create(AddressEnum mode) {
        switch (mode) {
            case IOTA:
                return new IoTANewAddress();
            default:
                return null;
        }
    }

}