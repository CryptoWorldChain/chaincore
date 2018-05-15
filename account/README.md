# 类库
## 引用
	compile ("org.brewchain:org.brewchain.frontend:{version}")
## 注入
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper accountHelper;

	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;

	@ActorRequire(name = "Block_Helper", scope = "global")
	BlockHelper blockHelper;

## 定义
Account     [act.proto](http://cwvi.club:9999/chaincore/account/blob/master/src/main/proto/act.proto)

Transaction [tx.proto](http://cwvi.club:9999/chaincore/account/blob/master/src/main/proto/tx.proto)

Block       [block.proto](http://cwvi.club:9999/chaincore/account/blob/master/src/main/proto/block.proto)

Sys         [sys.proto](http://cwvi.club:9999/chaincore/account/blob/master/src/main/proto/sys.proto)
# 账户API
## 创建账户
	act/pbcac.do
## 获取账户信息
	act/pbgac.do
## 添加CryptoToken (new)
	sys/pbacb.do

# 交易API
## 创建多重交易
	txt/pbmtx.do
## 获取未广播交易
	txt/pbgut.do
## 广播交易
	txt/pbayc.do
## 获取交易
	txt/pbgtx.do

# 区块API
## 生成区块
	btc/pbgbc.do
## 广播区块
	btc/pbsbc.do