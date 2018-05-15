package org.brewchain.account.util;

import java.util.ArrayList;

public class RLPList extends ArrayList<RLPElement> implements RLPElement {

    byte[] rlpData;

    public void setRLPData(byte[] rlpData) {
        this.rlpData = rlpData;
    }

    public byte[] getRLPData() {
        return rlpData;
    }

    public static void recursivePrint(RLPElement element) {

        if (element == null)
            throw new RuntimeException("RLPElement object can't be null");
        if (element instanceof RLPList) {

            RLPList rlpList = (RLPList) element;
            for (RLPElement singleElement : rlpList)
                recursivePrint(singleElement);
        } else {
            String hex = ByteUtil.toHexString(element.getRLPData());
        }
    }
}
