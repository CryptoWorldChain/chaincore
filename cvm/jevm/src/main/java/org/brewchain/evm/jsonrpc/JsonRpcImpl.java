package org.brewchain.evm.jsonrpc;

//import static org.brewchain.core.crypto.HashUtil.sha3;
import static org.brewchain.evm.jsonrpc.TypeConverter.StringHexToBigInteger;
import static org.brewchain.evm.jsonrpc.TypeConverter.StringHexToByteArray;
import static org.brewchain.evm.jsonrpc.TypeConverter.toJsonHex;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.transaction.Transaction;
import org.apache.maven.model.Repository;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Block;
import org.brewchain.account.gens.Block.BlockHeader;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.util.ByteUtil;
import org.brewchain.core.crypto.ECKey;
import org.brewchain.core.crypto.HashUtil;
import org.brewchain.evm.call.CallTransaction;
import org.brewchain.evm.exec.TransactionExecutor;
import org.brewchain.evm.exec.tx.MultiTransactionTask;
import org.brewchain.evm.solidity.compiler.SolidityCompiler;
import org.brewchain.evm.utils.ByteArrayWrapper;

import com.google.protobuf.ByteString;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Instantiate(name = "rpc_Impl")
public class JsonRpcImpl implements JsonRpc {
	
    SolidityCompiler solidityCompiler;
    
    @ActorRequire(name = "Transaction_Helper", scope = "global") @Setter
	TransactionHelper transactionHelper;
    
    public class BinaryCallArguments {
        public long nonce;
        public long gasPrice;
        public long gasLimit;
        public String toAddress;
        public long value;
        public byte[] data;
        public void setArguments(CallArguments args) throws Exception {
            nonce = 0;
            if (args.nonce != null && args.nonce.length() != 0)
                nonce = JSonHexToLong(args.nonce);

            gasPrice = 0;
            if (args.gasPrice != null && args.gasPrice.length()!=0)
                gasPrice = JSonHexToLong(args.gasPrice);

            gasLimit = 4_000_000;
            if (args.gas != null && args.gas.length()!=0)
                gasLimit = JSonHexToLong(args.gas);

            toAddress = null;
            if (args.to != null && !args.to.isEmpty())
                toAddress = JSonHexToHex(args.to);

            value=0;
            if (args.value != null && args.value.length()!=0)
                value = JSonHexToLong(args.value);

            data = null;

            if (args.data != null && args.data.length()!=0)
                data = TypeConverter.StringHexToByteArray(args.data);
        }
    }
    
    long initialBlockNumber;

    Map<ByteArrayWrapper, Account.Builder> accounts = new HashMap<>();
    AtomicInteger filterCounter = new AtomicInteger(1);
    Map<Integer, Filter> installedFilters = new Hashtable<>();
    Map<ByteArrayWrapper, TransactionReceipt> pendingReceipts = Collections.synchronizedMap(new LRUMap<ByteArrayWrapper, TransactionReceipt>(1024));

//    public JsonRpcImpl(final BlockchainImpl blockchain, final CompositeEthereumListener compositeEthereumListener) {
//        this.blockchain = blockchain;
//        this.compositeEthereumListener = compositeEthereumListener;
//        initialBlockNumber = blockchain.getBestBlock().getNumber();
//
//        compositeEthereumListener.addListener(new EthereumListenerAdapter() {
//            
//            public void onBlock(Block block, List<TransactionReceipt> receipts) {
//                for (Filter filter : installedFilters.values()) {
//                    filter.newBlockReceived(block);
//                }
//            }
//
//            
//            public void onPendingTransactionsReceived(List<Transaction> transactions) {
//                for (Filter filter : installedFilters.values()) {
//                    for (Transaction tx : transactions) {
//                        filter.newPendingTx(tx);
//                    }
//                }
//            }
//
//            
//            public void onPendingTransactionUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block) {
//                ByteArrayWrapper txHashW = new ByteArrayWrapper(txReceipt.getTransaction().getHash());
//                if (state.isPending() || state == PendingTransactionState.DROPPED) {
//                    pendingReceipts.put(txHashW, txReceipt);
//                } else {
//                    pendingReceipts.remove(txHashW);
//                }
//            }
//        });
//
//    }

    public long JSonHexToLong(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Long.parseLong(x, 16);
    }

    public int JSonHexToInt(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Integer.parseInt(x, 16);
    }

    public String JSonHexToHex(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return x;
    }

//    public Block getBlockByJSonHash(String blockHash) throws Exception  {
//        byte[] bhash = TypeConverter.StringHexToByteArray(blockHash);
//        return worldManager.getBlockchain().getBlockByHash(bhash);
//    }

//    private Block getByJsonBlockId(String id) {
//        if ("earliest".equalsIgnoreCase(id)) {
//            return blockchain.getBlockByNumber(0);
//        } else if ("latest".equalsIgnoreCase(id)) {
//            return blockchain.getBestBlock();
//        } else if ("pending".equalsIgnoreCase(id)) {
//            return null;
//        } else {
//            long blockNumber = StringHexToBigInteger(id).longValue();
//            return blockchain.getBlockByNumber(blockNumber);
//        }
//    }

//    private Repository getRepoByJsonBlockId(String id) {
//        if ("pending".equalsIgnoreCase(id)) {
//            return pendingState.getRepository();
//        } else {
//            Block block = getByJsonBlockId(id);
//            return this.repository.getSnapshotTo(block.getStateRoot());
//        }
//    }

//    private List<Transaction> getTransactionsByJsonBlockId(String id) {
//        if ("pending".equalsIgnoreCase(id)) {
//            return pendingState.getPendingTransactions();
//        } else {
//            Block block = getByJsonBlockId(id);
//            return block != null ? block.getTransactionsList() : null;
//        }
//    }

    protected Account.Builder getAccount(String address) throws Exception {
        return accounts.get(new ByteArrayWrapper(StringHexToByteArray(address)));
    }

    protected Account addAccount(String seed) {
    		// TODO ;)
    		return addAccount(ECKey.fromPrivate(HashUtil.sha3(seed.getBytes())));
    }

    protected Account addAccount(ECKey key) {
        Account.Builder account = Account.newBuilder();
//        account.init(key);
        account.setAddress(ByteString.copyFrom(key.getAddress()));
        
        accounts.put(new ByteArrayWrapper(account.getAddress().toByteArray()), account);
        return account.build();
    }

    public String web3_clientVersion() {

//        String s = "EthereumJ" + "/v" + config.projectVersion() + "/" +
//                System.getProperty("os.name") + "/Java1.7/" + config.projectVersionModifier() + "-" + BuildInfo.buildHash;

        String s = "EthereumJ" + System.getProperty("os.name") + "/Java1.7";
        if (log.isDebugEnabled()) log.debug("web3_clientVersion(): " + s);
        return s;
    };

    public String  web3_sha3(String data) throws Exception {
        String s = null;
        try {
            byte[] result = HashUtil.sha3(TypeConverter.StringHexToByteArray(data));
            return s = TypeConverter.toJsonHex(result);
        } finally {
            if (log.isDebugEnabled()) log.debug("web3_sha3(" + data + "): " + s);
        }
    }

    public String net_version() {
        String s = null;
        try {
            return s = eth_protocolVersion();
        } finally {
            if (log.isDebugEnabled()) log.debug("net_version(): " + s);
        }
    }

//    public String net_peerCount(){
//        String s = null;
//        try {
//            int n = channelManager.getActivePeers().size();
//            return s = TypeConverter.toJsonHex(n);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("net_peerCount(): " + s);
//        }
//    }

//    public boolean net_listening() {
//        Boolean s = null;
//        try {
//            return s = peerServer.isListening();
//        }finally {
//            if (log.isDebugEnabled()) log.debug("net_listening(): " + s);
//        }
//    }

    public String eth_protocolVersion(){
        String s = null;
        try {
            int version = 0;
//            for (Capability capability : configCapabilities.getConfigCapabilities()) {
//                if (capability.isEth()) {
//                    version = max(version, capability.getVersion());
//                }
//            }
            return s = Integer.toString(version);
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_protocolVersion(): " + s);
        }
    }

    public SyncingResult eth_syncing(){
        SyncingResult s = new SyncingResult();
        try {
            s.startingBlock = TypeConverter.toJsonHex(initialBlockNumber);
//            s.currentBlock = TypeConverter.toJsonHex(blockchain.getBestBlock().getNumber());
//            s.highestBlock = TypeConverter.toJsonHex(syncManager.getLastKnownBlockNumber());

            return s;
        }finally {
            if (log.isDebugEnabled()) log.debug("eth_syncing(): " + s);
        }
    };

//    public String eth_coinbase() {
//        String s = null;
//        try {
//            return s = toJsonHex(blockchain.getMinerCoinbase());
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_coinbase(): " + s);
//        }
//    }

//    public boolean eth_mining() {
//        Boolean s = null;
//        try {
//            return s = blockMiner.isMining();
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_mining(): " + s);
//        }
//    }


    public String eth_hashrate() {
        String s = null;
        try {
            return s = null;
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_hashrate(): " + s);
        }
    }

//    public String eth_gasPrice(){
//        String s = null;
//        try {
//            return s = TypeConverter.toJsonHex(eth.getGasPrice());
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_gasPrice(): " + s);
//        }
//    }

    public String[] eth_accounts() {
        String[] s = null;
        try {
            return s = personal_listAccounts();
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_accounts(): " + Arrays.toString(s));
        }
    }

//    public String eth_blockNumber(){
//        String s = null;
//        try {
//            Block bestBlock = blockchain.getBestBlock();
//            long b = 0;
//            if (bestBlock != null) {
//                b = bestBlock.getNumber();
//            }
//            return s = TypeConverter.toJsonHex(b);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_blockNumber(): " + s);
//        }
//    }


//    public String eth_getBalance(String address, String blockId) throws Exception {
//        String s = null;
//        try {
//            byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
//            BigInteger balance = getRepoByJsonBlockId(blockId).getBalance(addressAsByteArray);
//            return s = TypeConverter.toJsonHex(balance);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getBalance(" + address + ", " + blockId + "): " + s);
//        }
//    }

    public String eth_getBalance(String address) throws Exception {
        String s = null;
        try {
            return s = eth_getBalance(address, "latest");
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_getBalance(" + address + "): " + s);
        }
    }

//    
//    public String eth_getStorageAt(String address, String storageIdx, String blockId) throws Exception {
//        String s = null;
//        try {
//            byte[] addressAsByteArray = StringHexToByteArray(address);
//            DataWord storageValue = getRepoByJsonBlockId(blockId).
//                    getStorageValue(addressAsByteArray, new DataWord(StringHexToByteArray(storageIdx)));
//            return s = TypeConverter.toJsonHex(storageValue.getData());
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getStorageAt(" + address + ", " + storageIdx + ", " + blockId + "): " + s);
//        }
//    }

//    
//    public String eth_getTransactionCount(String address, String blockId) throws Exception {
//        String s = null;
//        try {
//            byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
//            BigInteger nonce = getRepoByJsonBlockId(blockId).getNonce(addressAsByteArray);
//            return s = TypeConverter.toJsonHex(nonce);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getTransactionCount(" + address + ", " + blockId + "): " + s);
//        }
//    }

//    public String eth_getBlockTransactionCountByHash(String blockHash) throws Exception {
//        String s = null;
//        try {
//            Block b = getBlockByJSonHash(blockHash);
//            if (b == null) return null;
//            long n = b.getTransactionsList().size();
//            return s = TypeConverter.toJsonHex(n);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getBlockTransactionCountByHash(" + blockHash + "): " + s);
//        }
//    }

//    public String eth_getBlockTransactionCountByNumber(String bnOrId) throws Exception {
//        String s = null;
//        try {
//            List<Transaction> list = getTransactionsByJsonBlockId(bnOrId);
//            if (list == null) return null;
//            long n = list.size();
//            return s = TypeConverter.toJsonHex(n);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getBlockTransactionCountByNumber(" + bnOrId + "): " + s);
//        }
//    }

//    public String eth_getUncleCountByBlockHash(String blockHash) throws Exception {
//        String s = null;
//        try {
//            Block b = getBlockByJSonHash(blockHash);
//            if (b == null) return null;
//            long n = b.getUncleList().size();
//            return s = TypeConverter.toJsonHex(n);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getUncleCountByBlockHash(" + blockHash + "): " + s);
//        }
//    }

//    public String eth_getUncleCountByBlockNumber(String bnOrId) throws Exception {
//        String s = null;
//        try {
//            Block b = getByJsonBlockId(bnOrId);
//            if (b == null) return null;
//            long n = b.getUncleList().size();
//            return s = TypeConverter.toJsonHex(n);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getUncleCountByBlockNumber(" + bnOrId + "): " + s);
//        }
//    }

//    public String eth_getCode(String address, String blockId) throws Exception {
//        String s = null;
//        try {
//            byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
//            byte[] code = getRepoByJsonBlockId(blockId).getCode(addressAsByteArray);
//            return s = TypeConverter.toJsonHex(code);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getCode(" + address + ", " + blockId + "): " + s);
//        }
//    }

//    public String eth_sign(String addr,String data) throws Exception {
//        String s = null;
//        try {
//            String ha = JSonHexToHex(addr);
//            Account account = getAccount(ha);
//
//            if (account==null)
//                throw new Exception("Inexistent account");
//
//            // Todo: is not clear from the spec what hash function must be used to sign
//            byte[] masgHash= HashUtil.sha3(TypeConverter.StringHexToByteArray(data));
//            ECKey.ECDSASignature signature = account.getEcKey().sign(masgHash);
//            // Todo: is not clear if result should be RlpEncoded or serialized by other means
//            byte[] rlpSig = RLP.encode(signature);
//            return s = TypeConverter.toJsonHex(rlpSig);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_sign(" + addr + ", " + data + "): " + s);
//        }
//    }

    
    public String eth_sendTransaction(CallArguments args) throws Exception {

        String s = null;
        try {
            Account.Builder account = getAccount(JSonHexToHex(args.from));

            if (account == null)
                throw new Exception("From address private key could not be found in this node");

            if (args.data != null && args.data.startsWith("0x"))
                args.data = args.data.substring(2);

//            MultiTransaction tx = new MultiTransaction(
//                    args.nonce != null ? StringHexToByteArray(args.nonce) : ByteUtil.bigIntegerToBytes(pendingState.getRepository().getNonce(account.getAddress())),
//                    args.gasPrice != null ? StringHexToByteArray(args.gasPrice) : ByteUtil.longToBytesNoLeadZeroes(eth.getGasPrice()),
//                    args.gas != null ? StringHexToByteArray(args.gas) : ByteUtil.longToBytes(90_000),
//                    args.to != null ? StringHexToByteArray(args.to) : ByteUtil.EMPTY_BYTE_ARRAY,
//                    args.value != null ? StringHexToByteArray(args.value) : ByteUtil.EMPTY_BYTE_ARRAY,
//                    args.data != null ? StringHexToByteArray(args.data) : ByteUtil.EMPTY_BYTE_ARRAY,
//                    eth.getChainIdForNextBlock());
//            tx.sign(account.getEcKey().getPrivKeyBytes());

//          args.nonce != null ? StringHexToByteArray(args.nonce) : ByteUtil.bigIntegerToBytes(pendingState.getRepository().getNonce(account.getAddress()));
//          args.gasPrice != null ? StringHexToByteArray(args.gasPrice) : ByteUtil.longToBytesNoLeadZeroes(eth.getGasPrice());
//          args.gas != null ? StringHexToByteArray(args.gas) : ByteUtil.longToBytes(90_000);
//          args.to != null ? StringHexToByteArray(args.to) : ByteUtil.EMPTY_BYTE_ARRAY;
//          args.value != null ? StringHexToByteArray(args.value) : ByteUtil.EMPTY_BYTE_ARRAY;
//          args.data != null ? StringHexToByteArray(args.data) : ByteUtil.EMPTY_BYTE_ARRAY;
            
            MultiTransaction.Builder tx = MultiTransaction.newBuilder();
            
            // TODO set tx object
            
            submitTransaction(tx,transactionHelper);

            return s = TypeConverter.toJsonHex(tx.getTxHash().toByteArray());
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_sendTransaction(" + args + "): " + s);
        }
    }
    public void submitTransaction(MultiTransaction.Builder transaction,TransactionHelper transactionHelper) {
    		
    		MultiTransactionTask txTask = new MultiTransactionTask(transaction, transactionHelper);
    		
//        final Future<List<Transaction>> listFuture =
//                TransactionExecutor.instance.submitTransaction(txTask);
        
//        pendingState.addPendingTransaction(transaction);
//        return new FutureAdapter<Transaction, List<Transaction>>(listFuture) {
//            @Override
//            protected Transaction adapt(List<Transaction> adapteeResult) throws ExecutionException {
//                return adapteeResult.get(0);
//            }
//        };
    }

//    public String eth_sendTransaction(String from, String to, String gas,
//                                      String gasPrice, String value,String data,String nonce) throws Exception {
//        String s = null;
//        try {
//        		MultiTransaction tx = new MultiTransaction(
//                    TypeConverter.StringHexToByteArray(nonce),
//                    TypeConverter.StringHexToByteArray(gasPrice),
//                    TypeConverter.StringHexToByteArray(gas),
//                    TypeConverter.StringHexToByteArray(to), /*receiveAddress*/
//                    TypeConverter.StringHexToByteArray(value),
//                    TypeConverter.StringHexToByteArray(data),
//                    eth.getChainIdForNextBlock());
//
//            Account account = getAccount(from);
//            if (account == null) throw new RuntimeException("No account " + from);
//
//            tx.sign(account.getEcKey());
//
//            eth.submitTransaction(tx);
//
//            return s = TypeConverter.toJsonHex(tx.getHash());
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_sendTransaction(" +
//                    "from = [" + from + "], to = [" + to + "], gas = [" + gas + "], gasPrice = [" + gasPrice +
//                    "], value = [" + value + "], data = [" + data + "], nonce = [" + nonce + "]" + "): " + s);
//        }
//    }

//    public String eth_sendRawTransaction(String rawData) throws Exception {
//        String s = null;
//        try {
//        		MultiTransaction tx = new MultiTransaction(StringHexToByteArray(rawData));
//            tx.verify();
//
//            eth.submitTransaction(tx);
//
//            return s = TypeConverter.toJsonHex(tx.getHash());
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_sendRawTransaction(" + rawData + "): " + s);
//        }
//    }

//    public TransactionReceipt createCallTxAndExecute(CallArguments args, Block block) throws Exception {
//        Repository repository = ((Repository) worldManager.getRepository())
//                .getSnapshotTo(block.getStateRoot())
//                .startTracking();
//
//        return createCallTxAndExecute(args, block, repository, worldManager.getBlockStore());
//    }

    public TransactionReceipt createCallTxAndExecute(CallArguments args, Block block, Repository repository, BlockHeader blockStore) throws Exception {
        BinaryCallArguments bca = new BinaryCallArguments();
        bca.setArguments(args);
        
        // TODO
        MultiTransaction tx = null;
//        MultiTransaction tx = CallTransaction.createRawTransaction(0,
//                bca.gasPrice,
//                bca.gasLimit,
//                bca.toAddress,
//                bca.value,
//                bca.data);

        // put mock signature if not present
//        if (tx.getSignature() == null) {
//            tx.sign(ECKey.fromPrivate(new byte[32]));
//        }

        try {
//            TransactionExecutor executor = new TransactionExecutor
//                    (tx, block.getCoinbase(), repository, blockStore,
//                            programInvokeFactory, block, new EthereumListenerAdapter(), 0)
//                    .withCommonConfig(commonConfig)
//                    .setLocalCall(true);
        	TransactionExecutor executor = new TransactionExecutor(tx, null, repository, blockStore,null, block, 0)
        									.withCommonConfig().setLocalCall(true);

            executor.init();
            executor.execute();
            executor.go();
//            executor.finalization();

            return executor.getReceipt();
        } finally {
//            repository.rollback();
        }
    }

    public String eth_call(CallArguments args, String bnOrId) throws Exception {

        String s = null;
        try {
            TransactionReceipt res;
            if ("pending".equals(bnOrId)) {
//                Block pendingBlock = blockchain.createNewBlock(blockchain.getBestBlock(), pendingState.getPendingTransactions(), Collections.<BlockHeader>emptyList());
//                res = createCallTxAndExecute(args, pendingBlock, pendingState.getRepository(), worldManager.getBlockStore());
            } else {
//                res = createCallTxAndExecute(args, getByJsonBlockId(bnOrId));
            }
//            return s = TypeConverter.toJsonHex(res.getExecutionResult());
            return null;
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_call(" + args + "): " + s);
        }
    }

//    public String eth_estimateGas(CallArguments args) throws Exception {
//        String s = null;
//        try {
//            TransactionReceipt res = createCallTxAndExecute(args, blockchain.getBestBlock());
//            return s = TypeConverter.toJsonHex(res.getGasUsed());
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_estimateGas(" + args + "): " + s);
//        }
//    }


    public BlockResult getBlockResult(Block b, boolean fullTx) {
        if (b==null)
            return null;
//        boolean isPending = ByteUtil.byteArrayToLong(b.getNonce()) == 0;
        BlockResult br = new BlockResult();
//        br.number = isPending ? null : TypeConverter.toJsonHex(b.getNumber());
//        br.hash = isPending ? null : TypeConverter.toJsonHex(b.getHash());
//        br.parentHash = TypeConverter.toJsonHex(b.getParentHash());
//        br.nonce = isPending ? null : TypeConverter.toJsonHex(b.getNonce());
//        br.sha3Uncles= TypeConverter.toJsonHex(b.getUnclesHash());
//        br.logsBloom = isPending ? null : TypeConverter.toJsonHex(b.getLogBloom());
//        br.transactionsRoot =TypeConverter.toJsonHex(b.getTxTrieRoot());
//        br.stateRoot = TypeConverter.toJsonHex(b.getStateRoot());
//        br.receiptsRoot =TypeConverter.toJsonHex(b.getReceiptsRoot());
//        br.miner = isPending ? null : TypeConverter.toJsonHex(b.getCoinbase());
//        br.difficulty = TypeConverter.toJsonHex(b.getDifficulty());
//        br.totalDifficulty = TypeConverter.toJsonHex(blockchain.getTotalDifficulty());
//        if (b.getExtraData() != null)
//            br.extraData =TypeConverter.toJsonHex(b.getExtraData());
//        br.size = TypeConverter.toJsonHex(b.getEncoded().length);
//        br.gasLimit =TypeConverter.toJsonHex(b.getGasLimit());
//        br.gasUsed =TypeConverter.toJsonHex(b.getGasUsed());
//        br.timestamp =TypeConverter.toJsonHex(b.getTimestamp());
//
//        List<Object> txes = new ArrayList<>();
//        if (fullTx) {
//            for (int i = 0; i < b.getTransactionsList().size(); i++) {
//                txes.add(new TransactionResultDTO(b, i, b.getTransactionsList().get(i)));
//            }
//        } else {
//            for (Transaction tx : b.getTransactionsList()) {
//                txes.add(toJsonHex(tx.getHash()));
//            }
//        }
//        br.transactions = txes.toArray();
//
//        List<String> ul = new ArrayList<>();
//        for (BlockHeader header : b.getUncleList()) {
//            ul.add(toJsonHex(header.getHash()));
//        }
//        br.uncles = ul.toArray(new String[ul.size()]);

        return br;
    }

//    public BlockResult eth_getBlockByHash(String blockHash,Boolean fullTransactionObjects) throws Exception {
//        BlockResult s = null;
//        try {
//            Block b = getBlockByJSonHash(blockHash);
//            return getBlockResult(b, fullTransactionObjects);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getBlockByHash(" +  blockHash + ", " + fullTransactionObjects + "): " + s);
//        }
//    }

//    public BlockResult eth_getBlockByNumber(String bnOrId,Boolean fullTransactionObjects) throws Exception {
//        BlockResult s = null;
//        try {
//            Block b;
//            if ("pending".equalsIgnoreCase(bnOrId)) {
//                b = blockchain.createNewBlock(blockchain.getBestBlock(), pendingState.getPendingTransactions(), Collections.<BlockHeader>emptyList());
//            } else {
//                b = getByJsonBlockId(bnOrId);
//            }
//            return s = (b == null ? null : getBlockResult(b, fullTransactionObjects));
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getBlockByNumber(" +  bnOrId + ", " + fullTransactionObjects + "): " + s);
//        }
//    }

//    public TransactionResultDTO eth_getTransactionByHash(String transactionHash) throws Exception {
//        TransactionResultDTO s = null;
//        try {
//            byte[] txHash = StringHexToByteArray(transactionHash);
//            Block block = null;
//
//            TransactionInfo txInfo = blockchain.getTransactionInfo(txHash);
//
//            if (txInfo == null) {
//                TransactionReceipt receipt = pendingReceipts.get(new ByteArrayWrapper(txHash));
//
//                if (receipt == null) {
//                    return null;
//                }
//                txInfo = new TransactionInfo(receipt);
//            } else {
//                block = blockchain.getBlockByHash(txInfo.getBlockHash());
//                // need to return txes only from main chain
//                Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
//                if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
//                    return null;
//                }
//                txInfo.setTransaction(block.getTransactionsList().get(txInfo.getIndex()));
//            }
//
//            return s = new TransactionResultDTO(block, txInfo.getIndex(), txInfo.getReceipt().getTransaction());
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getTransactionByHash(" + transactionHash + "): " + s);
//        }
//    }

//    public TransactionResultDTO eth_getTransactionByBlockHashAndIndex(String blockHash,String index) throws Exception {
//        TransactionResultDTO s = null;
//        try {
//            Block b = getBlockByJSonHash(blockHash);
//            if (b == null) return null;
//            int idx = JSonHexToInt(index);
//            if (idx >= b.getTransactionsList().size()) return null;
//            Transaction tx = b.getTransactionsList().get(idx);
//            return s = new TransactionResultDTO(b, idx, tx);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getTransactionByBlockHashAndIndex(" + blockHash + ", " + index + "): " + s);
//        }
//    }

//    public TransactionResultDTO eth_getTransactionByBlockNumberAndIndex(String bnOrId, String index) throws Exception {
//        TransactionResultDTO s = null;
//        try {
//            Block b = getByJsonBlockId(bnOrId);
//            List<Transaction> txs = getTransactionsByJsonBlockId(bnOrId);
//            if (txs == null) return null;
//            int idx = JSonHexToInt(index);
//            if (idx >= txs.size()) return null;
//            Transaction tx = txs.get(idx);
//            return s = new TransactionResultDTO(b, idx, tx);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getTransactionByBlockNumberAndIndex(" + bnOrId + ", " + index + "): " + s);
//        }
//    }

//    public TransactionReceiptDTO eth_getTransactionReceipt(String transactionHash) throws Exception {
//        TransactionReceiptDTO s = null;
//        try {
//            byte[] hash = TypeConverter.StringHexToByteArray(transactionHash);
//
//            TransactionReceipt pendingReceipt = pendingReceipts.get(new ByteArrayWrapper(hash));
//
//            TransactionInfo txInfo;
//            Block block;
//
//            if (pendingReceipt != null) {
//                txInfo = new TransactionInfo(pendingReceipt);
//                block = null;
//            } else {
//                txInfo = blockchain.getTransactionInfo(hash);
//
//                if (txInfo == null)
//                    return null;
//
//                block = blockchain.getBlockByHash(txInfo.getBlockHash());
//
//                // need to return txes only from main chain
//                Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
//                if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
//                    return null;
//                }
//            }
//
//            return s = new TransactionReceiptDTO(block, txInfo);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getTransactionReceipt(" + transactionHash + "): " + s);
//        }
//    }

//    
//    public TransactionReceiptDTOExt ethj_getTransactionReceipt(String transactionHash) throws Exception {
//        TransactionReceiptDTOExt s = null;
//        try {
//            byte[] hash = TypeConverter.StringHexToByteArray(transactionHash);
//
//            TransactionReceipt pendingReceipt = pendingReceipts.get(new ByteArrayWrapper(hash));
//
//            TransactionInfo txInfo;
//            Block block;
//
//            if (pendingReceipt != null) {
//                txInfo = new TransactionInfo(pendingReceipt);
//                block = null;
//            } else {
//                txInfo = blockchain.getTransactionInfo(hash);
//
//                if (txInfo == null)
//                    return null;
//
//                block = blockchain.getBlockByHash(txInfo.getBlockHash());
//
//                // need to return txes only from main chain
//                Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
//                if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
//                    return null;
//                }
//            }
//
//            return s = new TransactionReceiptDTOExt(block, txInfo);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getTransactionReceipt(" + transactionHash + "): " + s);
//        }
//    }

//    
//    public BlockResult eth_getUncleByBlockHashAndIndex(String blockHash, String uncleIdx) throws Exception {
//        BlockResult s = null;
//        try {
//            Block block = blockchain.getBlockByHash(StringHexToByteArray(blockHash));
//            if (block == null) return null;
//            int idx = JSonHexToInt(uncleIdx);
//            if (idx >= block.getUncleList().size()) return null;
//            BlockHeader uncleHeader = block.getUncleList().get(idx);
//            Block uncle = blockchain.getBlockByHash(uncleHeader.getHash());
//            if (uncle == null) {
//                uncle = new Block(uncleHeader, Collections.<Transaction>emptyList(), Collections.<BlockHeader>emptyList());
//            }
//            return s = getBlockResult(uncle, false);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getUncleByBlockHashAndIndex(" + blockHash + ", " + uncleIdx + "): " + s);
//        }
//    }

//    
//    public BlockResult eth_getUncleByBlockNumberAndIndex(String blockId, String uncleIdx) throws Exception {
//        BlockResult s = null;
//        try {
//            Block block = getByJsonBlockId(blockId);
//            return s = block == null ? null :
//                    eth_getUncleByBlockHashAndIndex(toJsonHex(block.getHash()), uncleIdx);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_getUncleByBlockNumberAndIndex(" + blockId + ", " + uncleIdx + "): " + s);
//        }
//    }

    
    public String[] eth_getCompilers() {
        String[] s = null;
        try {
            return s = new String[] {"solidity"};
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_getCompilers(): " + Arrays.toString(s));
        }
    }

    
    public CompilationResult eth_compileLLL(String contract) {
        throw new UnsupportedOperationException("LLL compiler not supported");
    }

    
    public CompilationResult eth_compileSolidity(String contract) throws Exception {
        CompilationResult s = null;
        try {
            SolidityCompiler.Result res = solidityCompiler.compileSrc(
                    contract.getBytes(), true, true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
            if (res.isFailed()) {
                throw new RuntimeException("Compilation error: " + res.errors);
            }
            org.brewchain.evm.solidity.compiler.CompilationResult result = org.brewchain.evm.solidity.compiler.CompilationResult.parse(res.output);
            CompilationResult ret = new CompilationResult();
            org.brewchain.evm.solidity.compiler.CompilationResult.ContractMetadata contractMetadata = result.contracts.values().iterator().next();
            ret.code = toJsonHex(contractMetadata.bin);
            ret.info = new CompilationInfo();
            ret.info.source = contract;
            ret.info.language = "Solidity";
            ret.info.languageVersion = "0";
            ret.info.compilerVersion = result.version;
            ret.info.abiDefinition = new CallTransaction.Contract(contractMetadata.abi).functions;
            return s = ret;
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_compileSolidity(" + contract + ")" + s);
        }
    }

    
    public CompilationResult eth_compileSerpent(String contract){
        throw new UnsupportedOperationException("Serpent compiler not supported");
    }

    
    public String eth_resend() {
        throw new UnsupportedOperationException("JSON RPC method eth_resend not implemented yet");
    }

    
    public String eth_pendingTransactions() {
        throw new UnsupportedOperationException("JSON RPC method eth_pendingTransactions not implemented yet");
    }

    static class Filter {
        static final int MAX_EVENT_COUNT = 1024; // prevent OOM when Filers are forgotten
        static abstract class FilterEvent {
            public abstract Object getJsonEventObject();
        }
        List<FilterEvent> events = new LinkedList<>();

        public synchronized boolean hasNew() { return !events.isEmpty();}

        public synchronized Object[] poll() {
            Object[] ret = new Object[events.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = events.get(i).getJsonEventObject();
            }
            this.events.clear();
            return ret;
        }

        protected synchronized void add(FilterEvent evt) {
            events.add(evt);
            if (events.size() > MAX_EVENT_COUNT) events.remove(0);
        }

        public void newBlockReceived(Block b) {}
        public void newPendingTx(Transaction tx) {}
    }

//    static class NewBlockFilter extends Filter {
//        class NewBlockFilterEvent extends FilterEvent {
//            public final Block b;
//            NewBlockFilterEvent(Block b) {this.b = b;}
//
//            
//            public String getJsonEventObject() {
//                return toJsonHex(b.getHash());
//            }
//        }
//
//        public void newBlockReceived(Block b) {
//            add(new NewBlockFilterEvent(b));
//        }
//    }

//    static class PendingTransactionFilter extends Filter {
//        class PendingTransactionFilterEvent extends FilterEvent {
//            private final MultiTransaction tx;
//
//            PendingTransactionFilterEvent(Transaction tx) {this.tx = tx;}
//
//            
//            public String getJsonEventObject() {
//                return toJsonHex(tx.getHash());
//            }
//        }
//
//        public void newPendingTx(Transaction tx) {
//            add(new PendingTransactionFilterEvent(tx));
//        }
//    }

    class JsonLogFilter extends Filter {
        class LogFilterEvent extends FilterEvent {
            private final LogFilterElement el;

            LogFilterEvent(LogFilterElement el) {
                this.el = el;
            }

            
            public LogFilterElement getJsonEventObject() {
                return el;
            }
        }

//        LogFilter logFilter;
//        boolean onNewBlock;
//        boolean onPendingTx;
//
//        public JsonLogFilter(LogFilter logFilter) {
//            this.logFilter = logFilter;
//        }
//
//        void onLogMatch(LogInfo logInfo, Block b, int txIndex, Transaction tx, int logIdx) {
//            add(new LogFilterEvent(new LogFilterElement(logInfo, b, txIndex, tx, logIdx)));
//        }
//
//        void onTransactionReceipt(TransactionReceipt receipt, Block b, int txIndex) {
//            if (logFilter.matchBloom(receipt.getBloomFilter())) {
//                int logIdx = 0;
//                for (LogInfo logInfo : receipt.getLogInfoList()) {
//                    if (logFilter.matchBloom(logInfo.getBloom()) && logFilter.matchesExactly(logInfo)) {
//                        onLogMatch(logInfo, b, txIndex, receipt.getTransaction(), logIdx);
//                    }
//                    logIdx++;
//                }
//            }
//        }
//
//        void onTransaction(Transaction tx, Block b, int txIndex) {
//            if (logFilter.matchesContractAddress(tx.getReceiveAddress())) {
//                TransactionInfo txInfo = blockchain.getTransactionInfo(tx.getHash());
//                onTransactionReceipt(txInfo.getReceipt(), b, txIndex);
//            }
//        }
//
//        void onBlock(Block b) {
//            if (logFilter.matchBloom(new Bloom(b.getLogBloom()))) {
//                int txIdx = 0;
//                for (Transaction tx : b.getTransactionsList()) {
//                    onTransaction(tx, b, txIdx);
//                    txIdx++;
//                }
//            }
//        }

//        
//        public void newBlockReceived(Block b) {
//            if (onNewBlock) onBlock(b);
//        }

        
        public void newPendingTx(Transaction tx) {
            // TODO add TransactionReceipt for PendingTx
//            if (onPendingTx)
        }
    }

//    
//    public String eth_newFilter(FilterRequest fr) throws Exception {
//        String str = null;
//        try {
//            LogFilter logFilter = new LogFilter();
//
//            if (fr.address instanceof String) {
//                logFilter.withContractAddress(StringHexToByteArray((String) fr.address));
//            } else if (fr.address instanceof String[]) {
//                List<byte[]> addr = new ArrayList<>();
//                for (String s : ((String[]) fr.address)) {
//                    addr.add(StringHexToByteArray(s));
//                }
//                logFilter.withContractAddress(addr.toArray(new byte[0][]));
//            }
//
//            if (fr.topics != null) {
//                for (Object topic : fr.topics) {
//                    if (topic == null) {
//                        logFilter.withTopic(null);
//                    } else if (topic instanceof String) {
//                        logFilter.withTopic(new DataWord(StringHexToByteArray((String) topic)).getData());
//                    } else if (topic instanceof String[]) {
//                        List<byte[]> t = new ArrayList<>();
//                        for (String s : ((String[]) topic)) {
//                            t.add(new DataWord(StringHexToByteArray(s)).getData());
//                        }
//                        logFilter.withTopic(t.toArray(new byte[0][]));
//                    }
//                }
//            }
//
//            JsonLogFilter filter = new JsonLogFilter(logFilter);
//            int id = filterCounter.getAndIncrement();
//            installedFilters.put(id, filter);
//
//            Block blockFrom = fr.fromBlock == null ? null : getByJsonBlockId(fr.fromBlock);
//            Block blockTo = fr.toBlock == null ? null : getByJsonBlockId(fr.toBlock);
//
//            if (blockFrom != null) {
//                // need to add historical data
//                blockTo = blockTo == null ? blockchain.getBestBlock() : blockTo;
//                for (long blockNum = blockFrom.getNumber(); blockNum <= blockTo.getNumber(); blockNum++) {
//                    filter.onBlock(blockchain.getBlockByNumber(blockNum));
//                }
//            }
//
//            // the following is not precisely documented
//            if ("pending".equalsIgnoreCase(fr.fromBlock) || "pending".equalsIgnoreCase(fr.toBlock)) {
//                filter.onPendingTx = true;
//            } else if ("latest".equalsIgnoreCase(fr.fromBlock) || "latest".equalsIgnoreCase(fr.toBlock)) {
//                filter.onNewBlock = true;
//            }
//
//            return str = toJsonHex(id);
//        } finally {
//            if (log.isDebugEnabled()) log.debug("eth_newFilter(" + fr + "): " + str);
//        }
//    }

    
    public String eth_newBlockFilter() {
        String s = null;
        try {
            int id = filterCounter.getAndIncrement();
//            installedFilters.put(id, new NewBlockFilter());
            return s = toJsonHex(id);
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_newBlockFilter(): " + s);
        }
    }

    
    public String eth_newPendingTransactionFilter() {
        String s = null;
        try {
            int id = filterCounter.getAndIncrement();
//            installedFilters.put(id, new PendingTransactionFilter());
            return s = toJsonHex(id);
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_newPendingTransactionFilter(): " + s);
        }
    }

    
    public boolean eth_uninstallFilter(String id) {
        Boolean s = null;
        try {
            if (id == null) return false;
            return s = installedFilters.remove(StringHexToBigInteger(id).intValue()) != null;
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_uninstallFilter(" + id + "): " + s);
        }
    }

    
    public Object[] eth_getFilterChanges(String id) {
        Object[] s = null;
        try {
            Filter filter = installedFilters.get(StringHexToBigInteger(id).intValue());
            if (filter == null) return null;
            return s = filter.poll();
        } finally {
            if (log.isDebugEnabled()) log.debug("eth_getFilterChanges(" + id + "): " + Arrays.toString(s));
        }
    }

    
    public Object[] eth_getFilterLogs(String id) {
        log.debug("eth_getFilterLogs ...");
        return eth_getFilterChanges(id);
    }

    
    public Object[] eth_getLogs(FilterRequest fr) throws Exception {
        log.debug("eth_getLogs ...");
        String id = eth_newFilter(fr);
        Object[] ret = eth_getFilterChanges(id);
        eth_uninstallFilter(id);
        return ret;
    }

    
    public String eth_getWork() {
        throw new UnsupportedOperationException("JSON RPC method eth_getWork not implemented yet");
    }

    
    public String eth_submitWork() {
        throw new UnsupportedOperationException("JSON RPC method eth_submitWork not implemented yet");
    }

    
    public String eth_submitHashrate() {
        throw new UnsupportedOperationException("JSON RPC method eth_submitHashrate not implemented yet");
    }

    
    public String db_putString() {
        throw new UnsupportedOperationException("JSON RPC method db_putString not implemented yet");
    }

    
    public String db_getString() {
        throw new UnsupportedOperationException("JSON RPC method db_getString not implemented yet");
    }

    
    public String db_putHex() {
        throw new UnsupportedOperationException("JSON RPC method db_putHex not implemented yet");
    }

    
    public String db_getHex() {
        throw new UnsupportedOperationException("JSON RPC method db_getHex not implemented yet");
    }

    
    public String shh_post() {
        throw new UnsupportedOperationException("JSON RPC method shh_post not implemented yet");
    }

    
    public String shh_version() {
        throw new UnsupportedOperationException("JSON RPC method shh_version not implemented yet");
    }

    
    public String shh_newIdentity() {
        throw new UnsupportedOperationException("JSON RPC method shh_newIdentity not implemented yet");
    }

    
    public String shh_hasIdentity() {
        throw new UnsupportedOperationException("JSON RPC method shh_hasIdentity not implemented yet");
    }

    
    public String shh_newGroup() {
        throw new UnsupportedOperationException("JSON RPC method shh_newGroup not implemented yet");
    }

    
    public String shh_addToGroup() {
        throw new UnsupportedOperationException("JSON RPC method shh_addToGroup not implemented yet");
    }

    
    public String shh_newFilter() {
        throw new UnsupportedOperationException("JSON RPC method shh_newFilter not implemented yet");
    }

    
    public String shh_uninstallFilter() {
        throw new UnsupportedOperationException("JSON RPC method shh_uninstallFilter not implemented yet");
    }

    
    public String shh_getFilterChanges() {
        throw new UnsupportedOperationException("JSON RPC method shh_getFilterChanges not implemented yet");
    }

    
    public String shh_getMessages() {
        throw new UnsupportedOperationException("JSON RPC method shh_getMessages not implemented yet");
    }

    
    public boolean admin_addPeer(String s) {
//        eth.connect(new Node(s));
        return true;
    }

    
    public String admin_exportChain() {
        throw new UnsupportedOperationException("JSON RPC method admin_exportChain not implemented yet");
    }

    
    public String admin_importChain() {
        throw new UnsupportedOperationException("JSON RPC method admin_importChain not implemented yet");
    }

    
    public String admin_sleepBlocks() {
        throw new UnsupportedOperationException("JSON RPC method admin_sleepBlocks not implemented yet");
    }

    
    public String admin_verbosity() {
        throw new UnsupportedOperationException("JSON RPC method admin_verbosity not implemented yet");
    }

    
    public String admin_setSolc() {
        throw new UnsupportedOperationException("JSON RPC method admin_setSolc not implemented yet");
    }

    
    public String admin_startRPC() {
        throw new UnsupportedOperationException("JSON RPC method admin_startRPC not implemented yet");
    }

    
    public String admin_stopRPC() {
        throw new UnsupportedOperationException("JSON RPC method admin_stopRPC not implemented yet");
    }

    
    public String admin_setGlobalRegistrar() {
        throw new UnsupportedOperationException("JSON RPC method admin_setGlobalRegistrar not implemented yet");
    }

    
    public String admin_setHashReg() {
        throw new UnsupportedOperationException("JSON RPC method admin_setHashReg not implemented yet");
    }

    
    public String admin_setUrlHint() {
        throw new UnsupportedOperationException("JSON RPC method admin_setUrlHint not implemented yet");
    }

    
    public String admin_saveInfo() {
        throw new UnsupportedOperationException("JSON RPC method admin_saveInfo not implemented yet");
    }

    
    public String admin_register() {
        throw new UnsupportedOperationException("JSON RPC method admin_register not implemented yet");
    }

    
    public String admin_registerUrl() {
        throw new UnsupportedOperationException("JSON RPC method admin_registerUrl not implemented yet");
    }

    
    public String admin_startNatSpec() {
        throw new UnsupportedOperationException("JSON RPC method admin_startNatSpec not implemented yet");
    }

    
    public String admin_stopNatSpec() {
        throw new UnsupportedOperationException("JSON RPC method admin_stopNatSpec not implemented yet");
    }

    
    public String admin_getContractInfo() {
        throw new UnsupportedOperationException("JSON RPC method admin_getContractInfo not implemented yet");
    }

    
    public String admin_httpGet() {
        throw new UnsupportedOperationException("JSON RPC method admin_httpGet not implemented yet");
    }

    
    public String admin_nodeInfo() {
        throw new UnsupportedOperationException("JSON RPC method admin_nodeInfo not implemented yet");
    }

    
    public String admin_peers() {
        throw new UnsupportedOperationException("JSON RPC method admin_peers not implemented yet");
    }

    
    public String admin_datadir() {
        throw new UnsupportedOperationException("JSON RPC method admin_datadir not implemented yet");
    }

    
    public String net_addPeer() {
        throw new UnsupportedOperationException("JSON RPC method net_addPeer not implemented yet");
    }

    
    public boolean miner_start() {
//        blockMiner.startMining();
        return true;
    }

    
    public boolean miner_stop() {
//        blockMiner.stopMining();
        return true;
    }

    
    public boolean miner_setEtherbase(String coinBase) throws Exception {
//        blockchain.setMinerCoinbase(TypeConverter.StringHexToByteArray(coinBase));
        return true;
    }

    
    public boolean miner_setExtra(String data) throws Exception {
//        blockchain.setMinerExtraData(TypeConverter.StringHexToByteArray(data));
        return true;
    }

    
    public boolean miner_setGasPrice(String newMinGasPrice) {
//        blockMiner.setMinGasPrice(TypeConverter.StringHexToBigInteger(newMinGasPrice));
        return true;
    }

    
    public boolean miner_startAutoDAG() {
        return false;
    }

    
    public boolean miner_stopAutoDAG() {
        return false;
    }

    
    public boolean miner_makeDAG() {
        return false;
    }

    
    public String miner_hashrate() {
        return "0x01";
    }

    
    public String debug_printBlock() {
        throw new UnsupportedOperationException("JSON RPC method debug_printBlock not implemented yet");
    }

    
    public String debug_getBlockRlp() {
        throw new UnsupportedOperationException("JSON RPC method debug_getBlockRlp not implemented yet");
    }

    
    public String debug_setHead() {
        throw new UnsupportedOperationException("JSON RPC method debug_setHead not implemented yet");
    }

    
    public String debug_processBlock() {
        throw new UnsupportedOperationException("JSON RPC method debug_processBlock not implemented yet");
    }

    
    public String debug_seedHash() {
        throw new UnsupportedOperationException("JSON RPC method debug_seedHash not implemented yet");
    }

    
    public String debug_dumpBlock() {
        throw new UnsupportedOperationException("JSON RPC method debug_dumpBlock not implemented yet");
    }

    
    public String debug_metrics() {
        throw new UnsupportedOperationException("JSON RPC method debug_metrics not implemented yet");
    }

    
    public String personal_newAccount(String seed) {
        String s = null;
        try {
            Account account = addAccount(seed);
            return s = toJsonHex(account.getAddress().toByteArray());
        } finally {
            if (log.isDebugEnabled()) log.debug("personal_newAccount(*****): " + s);
        }
    }

    
    public boolean personal_unlockAccount(String addr, String pass, String duration) {
        String s = null;
        try {
            return true;
        } finally {
            if (log.isDebugEnabled()) log.debug("personal_unlockAccount(" + addr + ", ***, " + duration + "): " + s);
        }
    }

    
    public String[] personal_listAccounts() {
        String[] ret = new String[accounts.size()];
        try {
            int i = 0;
            for (ByteArrayWrapper addr : accounts.keySet()) {
                ret[i++] = toJsonHex(addr.getData());
            }
            return ret;
        } finally {
            if (log.isDebugEnabled()) log.debug("personal_listAccounts(): " + Arrays.toString(ret));
        }
    }
    
    
    
    // MT 
    @Override
	public String net_peerCount() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public boolean net_listening() {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
	public String eth_coinbase() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public boolean eth_mining() {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
	public String eth_gasPrice() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_blockNumber() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_getBalance(String address, String block) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_getStorageAt(String address, String storageIdx, String blockId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_getTransactionCount(String address, String blockId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_getBlockTransactionCountByHash(String blockHash) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_getBlockTransactionCountByNumber(String bnOrId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_getUncleCountByBlockHash(String blockHash) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_getUncleCountByBlockNumber(String bnOrId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_getCode(String addr, String bnOrId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_sign(String addr, String data) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_sendTransaction(String from, String to, String gas, String gasPrice, String value, String data,
			String nonce) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_sendRawTransaction(String rawData) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public String eth_estimateGas(CallArguments args) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockResult eth_getBlockByHash(String blockHash, Boolean fullTransactionObjects) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockResult eth_getBlockByNumber(String bnOrId, Boolean fullTransactionObjects) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionResultDTO eth_getTransactionByHash(String transactionHash) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionResultDTO eth_getTransactionByBlockHashAndIndex(String blockHash, String index) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionResultDTO eth_getTransactionByBlockNumberAndIndex(String bnOrId, String index) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionReceiptDTO eth_getTransactionReceipt(String transactionHash) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionReceiptDTOExt ethj_getTransactionReceipt(String transactionHash) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockResult eth_getUncleByBlockHashAndIndex(String blockHash, String uncleIdx) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockResult eth_getUncleByBlockNumberAndIndex(String blockId, String uncleIdx) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String eth_newFilter(FilterRequest fr) throws Exception {
		// TODO Auto-generated method stub
/////////////////////////////////////////////////
		return null;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}