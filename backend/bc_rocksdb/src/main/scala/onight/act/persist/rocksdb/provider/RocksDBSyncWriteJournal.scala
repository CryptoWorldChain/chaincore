package net.atos.mts.akka.persistence.rocksdb

import java.io.File
import java.nio.ByteBuffer
import scala.collection.immutable.Seq
import org.rocksdb.Options
import org.rocksdb.ReadOptions
import org.rocksdb.RocksDB
import org.rocksdb.RocksIterator
import org.rocksdb.WriteBatch
import org.rocksdb.WriteOptions
import akka.actor.ActorLogging
import akka.persistence.PersistentConfirmation
import akka.persistence.PersistentId
import akka.persistence.PersistentRepr
import akka.persistence.journal.SyncWriteJournal
import akka.serialization.SerializationExtension
import scala.concurrent.Future
import org.rocksdb.CompressionType

/**
 * RocksDB key.
 */
private[rocksdb] final case class Key(
  persistenceId: Int,
  sequenceNr: Long,
  channelId: Int)
  
private[rocksdb] object Key {
  def keyToBytes(key: Key): Array[Byte] = {
    val bb = ByteBuffer.allocate(20)
    bb.putInt(key.persistenceId)
    bb.putLong(key.sequenceNr)
    bb.putInt(key.channelId)
    bb.array
  }

  def keyFromBytes(bytes: Array[Byte]): Key = {
    val bb = ByteBuffer.wrap(bytes)
    val aid = bb.getInt
    val snr = bb.getLong
    val cid = bb.getInt
    new Key(aid, snr, cid)
  }

  def counterKey(persistenceId: Int): Key = Key(persistenceId, 0L, 0)
  def counterToBytes(ctr: Long): Array[Byte] = ByteBuffer.allocate(8).putLong(ctr).array
  def counterFromBytes(bytes: Array[Byte]): Long = ByteBuffer.wrap(bytes).getLong

  def id(key: Key) = key.channelId
  def idKey(id: Int) = Key(1, 0L, id)
  def isIdKey(key: Key): Boolean = key.persistenceId == 1

  def deletionKey(persistenceId: Int, sequenceNr: Long): Key = Key(persistenceId, sequenceNr, 1)
  def isDeletionKey(key: Key): Boolean = key.channelId == 1
}

class RocksDBSyncWriteJournal extends SyncWriteJournal with ActorLogging {
    import Key._
    
    val configPath = "akka.persistence.journal.rocksdb.store"
    val config = context.system.settings.config.getConfig(configPath)
    
	val rocksdbOptions = new Options().setCreateIfMissing(true).setCompressionType(CompressionType.ZLIB_COMPRESSION)
	def rocksdbReadOptions = new ReadOptions().setVerifyChecksums(config.getBoolean("checksum"))
	val rocksdbWriteOptions = new WriteOptions().setSync(config.getBoolean("fsync"))
	val rocksdbDir = config.getString("dir")
	val rocksdb = RocksDB.open(rocksdbOptions, rocksdbDir)
	val serialization = SerializationExtension(context.system)
	
	private lazy val replayDispatcherId = config.getString("replay-dispatcher")
    private lazy val replayDispatcher = context.system.dispatchers.lookup(replayDispatcherId)
  
	private var idMap: Map[String, Int] = Map.empty
	private val idOffset = 10
	
	def writeMessages(messages: Seq[PersistentRepr]): Unit = {
	  log.debug(s"writeMessages for ${messages.size} persistent messages")
	  withBatch(batch => messages.foreach(message => addToMessageBatch(message, batch)))
	}
    
    def writeConfirmations(confirmations: Seq[PersistentConfirmation]): Unit = {
      log.debug(s"writeMessages for ${confirmations.size} persistent confirmations")
      withBatch(batch => confirmations.foreach(confirmation => addToConfirmationBatch(confirmation, batch)))
    }
    
    def deleteMessages(messageIds: Seq[PersistentId], permanent: Boolean) = withBatch { batch =>
      log.debug(s"deleteMessages for ${messageIds.size} persistent messages")
      messageIds foreach { messageId =>
        if (permanent) batch.remove(keyToBytes(Key(numericId(messageId.persistenceId), messageId.sequenceNr, 0)))
        else batch.put(keyToBytes(deletionKey(numericId(messageId.persistenceId), messageId.sequenceNr)), Array.emptyByteArray)
      }
    }
    
    def deleteMessagesTo(persistenceId: String, toSequenceNr: Long, permanent: Boolean) = withBatch { batch =>
      log.debug(s"deleteMessages from ${persistenceId} for ${toSequenceNr} persistent messages")
      val nid = numericId(persistenceId)

      // seek to first existing message
      val fromSequenceNr = withIterator { iter =>
        val startKey = Key(nid, 1L, 0)
        iter.seek(keyToBytes(startKey))
        if (iter.isValid()) keyFromBytes(iter.key()).sequenceNr else Long.MaxValue
      }
      
      fromSequenceNr to toSequenceNr foreach { sequenceNr =>
        if (permanent) batch.remove(keyToBytes(Key(nid, sequenceNr, 0))) // TODO: delete confirmations and deletion markers, if any.
        else batch.put(keyToBytes(deletionKey(nid, sequenceNr)), Array.emptyByteArray)
      }
    }

    def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
      log.debug(s"asyncReadHighestSequenceNr for persistenceId ${persistenceId} from ${fromSequenceNr}")
      val nid = numericId(persistenceId)
      Future(readHighestSequenceNr(nid))(replayDispatcher)
    }
    
    def readHighestSequenceNr(persistenceId: Int) = {
      val ro = rocksdbReadOptions
      log.debug(s"readHighestSequenceNr for persistenceId ${persistenceId}")
      try {
        rocksdb.get(ro, keyToBytes(counterKey(persistenceId))) match {
          case null  ⇒ 0L
          case bytes ⇒ counterFromBytes(bytes)
        }
      } finally {
        ro.dispose()
      }
    }
    
    def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(replayCallback: PersistentRepr ⇒ Unit): Future[Unit] = {
      log.debug(s"asyncReplayMessages for persistenceId ${persistenceId} from ${fromSequenceNr} to ${toSequenceNr} with a max ${max}")
      val nid = numericId(persistenceId)
      Future(replayMessages(nid, fromSequenceNr: Long, toSequenceNr, max: Long)(replayCallback))(replayDispatcher)
    }
    
    def persistentFromBytes(a: Array[Byte]): PersistentRepr = serialization.deserialize(a, classOf[PersistentRepr]).get
    
    def replayMessages(persistenceId: Int, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(replayCallback: PersistentRepr ⇒ Unit): Unit = {
      @scala.annotation.tailrec
      def go(iter: RocksIterator, key: Key, ctr: Long, replayCallback: PersistentRepr ⇒ Unit) {
        if (iter.isValid()) {
          val nextKey = keyFromBytes(iter.key())
          if (nextKey.sequenceNr > toSequenceNr) {
            // end iteration here
          } else if (nextKey.channelId != 0) {
            // phantom confirmation (just advance iterator)
            iter.next()
            go(iter, nextKey, ctr, replayCallback)
          } else if (key.persistenceId == nextKey.persistenceId) {
            val msg = persistentFromBytes(iter.value())
            val del = deletion(iter, nextKey)
            val cnf = confirms(iter, nextKey, Nil)
            if (ctr < max) {
              log.debug(s"replay message ${msg.persistenceId},${msg.sequenceNr} with confirms=${cnf.size} and deleted=${del}")
              replayCallback(msg.update(confirms = cnf, deleted = del))
              iter.next()
              go(iter, nextKey, ctr + 1L, replayCallback)
            }
          }
        }
      }

      @scala.annotation.tailrec
      def confirms(iter: RocksIterator, key: Key, channelIds: List[String]): List[String] = {
        if (hasNext(iter)) {
          val nextKey = keyFromBytes(peekNextKey(iter))
          if (key.persistenceId == nextKey.persistenceId && key.sequenceNr == nextKey.sequenceNr) {
            val nextValue = new String(peekNextValue(iter), "UTF-8")
            iter.next()
            confirms(iter, nextKey, nextValue :: channelIds)
          } else channelIds
        } else channelIds
      }

      def deletion(iter: RocksIterator, key: Key): Boolean = {
        if (hasNext(iter)) {
          val nextKey = keyFromBytes(peekNextKey(iter))
          if (key.persistenceId == nextKey.persistenceId && key.sequenceNr == nextKey.sequenceNr && isDeletionKey(nextKey)) {
            iter.next()
            true
          } else false
        } else false
      }

      withIterator { iter =>
        val startKey = Key(persistenceId, if (fromSequenceNr < 1L) 1L else fromSequenceNr, 0)
        iter.seek(keyToBytes(startKey))
        go(iter, startKey, 0L, replayCallback)
      }
    }
    
    def hasNext(iter: RocksIterator): Boolean = {
      if(!iter.isValid())
        false
      iter.next()
      val valid = iter.isValid()
      iter.prev()
      valid
    }
    
    def peekNextKey(iter: RocksIterator): Array[Byte] = {
      iter.next()
      val nextkey = iter.key()
      iter.prev()
      nextkey
    }
    
    def peekNextValue(iter: RocksIterator): Array[Byte] = {
      iter.next()
      val nextvalue = iter.value()
      iter.prev()
      nextvalue
    }
    
    
    def withIterator[R](body: RocksIterator => R): R = {
      val ro = rocksdbReadOptions
      val iterator = rocksdb.newIterator()
      try {
        body(iterator)
      } finally {
        iterator.dispose()
        ro.dispose()
      }
    }
	
	def withBatch[R](body: WriteBatch => R): R = {
	  val batch = new WriteBatch
	  try {
	    val r = body(batch)
	    rocksdb.write(rocksdbWriteOptions,batch)
	    r
	  } finally {
	    batch.dispose()
	  }
	}
	
	def persistentToBytes(p: PersistentRepr): Array[Byte] = serialization.serialize(p).get
	
	private def addToMessageBatch(persistent: PersistentRepr, batch: WriteBatch): Unit = {
	  val nid = numericId(persistent.persistenceId)
	  batch.put(keyToBytes(counterKey(nid)), counterToBytes(persistent.sequenceNr))
	  batch.put(keyToBytes(Key(nid, persistent.sequenceNr, 0)), persistentToBytes(persistent))
	}
	
	private def addToConfirmationBatch(confirmation: PersistentConfirmation, batch: WriteBatch): Unit = {
      val npid = numericId(confirmation.persistenceId)
      val ncid = numericId(confirmation.channelId)
      batch.put(keyToBytes(Key(npid, confirmation.sequenceNr, ncid)), confirmation.channelId.getBytes("UTF-8"))
    }
	
	/**
     * Get the mapped numeric id for the specified persistent actor or channel `id`. Creates and
     * stores a new mapping if necessary.
     */
    def numericId(id: String): Int = idMap.get(id) match {
      case None => writeIdMapping(id, idMap.size + idOffset)
      case Some(v) => v
    }
    
	private def writeIdMapping(id: String, numericId: Int): Int = {
	  idMap = idMap + (id -> numericId)
      rocksdb.put(keyToBytes(idKey(numericId)), id.getBytes("UTF-8"))
      numericId
    }
	
	override def preStart() {
	  log.debug("rocksdb persistence starts")
      super.preStart()
    }

	override def postStop() {
	  rocksdbWriteOptions.dispose()
	  rocksdbOptions.dispose()
      rocksdb.close()
      log.debug("rocksdb persistence is stopped")
      super.postStop()
    }
}