package org.fc.brewchain.bcapi.notify;

import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

public interface INotifyListener {
	/**
	 * 获取需要监听的gcmd值
	 * 
	 * @return
	 */
	public String getGCMD();

	public boolean onNotify(String gcmd, FramePacket packet, CompleteHandler handler);

}
