/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.brewchain.evm.exec.tx;

import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Tx.MultiTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.Thread.sleep;

/**
 * @author Roman Mandeleil
 * @since 23.05.2014
 */
public class MultiTransactionTask implements Callable<List<MultiTransaction.Builder>> {

    private static final Logger logger = LoggerFactory.getLogger("net");

    private final List<MultiTransaction.Builder> tx;
    private final TransactionHelper transactionHelper;
//    private final ChannelManager channelManager;
//    private final Channel receivedFrom;

    public MultiTransactionTask(MultiTransaction.Builder tx, TransactionHelper transactionHelper) {
        this(Collections.singletonList(tx), transactionHelper);
    }

    public MultiTransactionTask(List<MultiTransaction.Builder> tx, TransactionHelper transactionHelper) {
//        this(tx, channelManager, null);
    		this.tx = tx;
    		this.transactionHelper = transactionHelper;
    }

//    public MultiTransactionTask(List<MultiTransaction.Builder> tx, ChannelManager channelManager,Channel receivedFrom) {
//        this.tx = tx;
//        this.channelManager = channelManager;
//        this.receivedFrom = receivedFrom;
//    }

    @Override
    public List<MultiTransaction.Builder> call() throws Exception {

        try {
            logger.info("submit tx: {}", tx.toString());
//            channelManager.sendTransaction(tx, receivedFrom);
            for(MultiTransaction.Builder t : tx) {
    				transactionHelper.CreateMultiTransaction(t);
            }
            return tx;
        } catch (Exception e) {
            logger.warn("Exception caught: {}", e);
        }
        return null;
    }
}
