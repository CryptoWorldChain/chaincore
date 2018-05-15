package org.fc.brewchain.bcapi;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

import onight.tfw.otransio.api.beans.ExtHeader;
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.SerializerFactory;

/**
 * for BC packet
 * 
 * @author brew
 *
 */
public class BCPacket extends FramePacket {
	protected ExtHeader genExtHeader() {
		return new BCExtHeader();
	}

	public static BCPacket buildSyncFrom(Message body, String cmd, String module) {
		BCPacket ret = new BCPacket();
		ret.setFbody(body);
		FixHeader fh = new FixHeader();
		fh.setCmd(cmd);
		fh.setVer('b');
		fh.setSync(true);
		fh.setModule(module);
		fh.setEnctype(SerializerFactory.SERIALIZER_PROTOBUF);
		ret.setFixHead(fh);
		return ret;
	}
	
	public static BCPacket buildSyncFrom(byte[] body, String cmd, String module) {
		BCPacket ret = new BCPacket();
		ret.setBody(body);
		FixHeader fh = new FixHeader();
		fh.setCmd(cmd);
		fh.setVer('b');
		fh.setSync(true);
		fh.setModule(module);
		fh.setEnctype(SerializerFactory.SERIALIZER_PROTOBUF);
		ret.setFixHead(fh);
		return ret;
	}
	
	public static BCPacket buildAsyncFrom(byte[] body, String cmd, String module) {
		BCPacket ret = buildSyncFrom(body,cmd,module);
		ret.getFixHead().setSync(false);
		return ret;
	}
	
	public static BCPacket buildAsyncFrom(Message body, String cmd, String module) {
		BCPacket ret = buildSyncFrom(body,cmd,module);
		ret.getFixHead().setSync(false);
		return ret;
	}

}
