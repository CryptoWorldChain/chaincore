package org.fc.brewchain.bcapi.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;

@Component
@Instantiate(name = "filterManager")
@Provides(specifications = ActorService.class, properties = {
		@StaticServiceProperty(name = "name", value = "filterManager", type = "java.lang.String") })
@Slf4j
public class CMDWhiteBoard implements ActorService {

	BundleContext btx;
	PropHelper helper;

	ConcurrentHashMap<String, List<INotifyListener>> listensByGCMD = new ConcurrentHashMap<>();

	public CMDWhiteBoard(BundleContext btx) {
		this.btx = btx;
		helper = new PropHelper(btx);
	}

	public void notify(String gcmd, FramePacket packet, CompleteHandler handler) {
		List<INotifyListener> lnrs = listensByGCMD.get(gcmd);
		if (lnrs != null) {
			for (INotifyListener lnr : lnrs) {
				lnr.onNotify(gcmd, packet, handler);
			}
		}
	}

	@Bind(aggregate = true)
	public void registerLnr(INotifyListener lnr) {
		log.debug("bindListener::" + lnr);
		if(StringUtils.isNotBlank(lnr.getGCMD())){
			List<INotifyListener> lnrs = listensByGCMD.get(lnr.getGCMD());
			if (lnrs == null) {
				lnrs = new ArrayList<>();
			}
			lnrs.add(lnr);
		}
	}

	@Unbind(aggregate = true)
	public void unregisterLnr(INotifyListener lnr) {
		log.debug("unbindListener::" + lnr);
		if (StringUtils.isNotBlank(lnr.getGCMD())) {
			List<INotifyListener> lnrs = listensByGCMD.get(lnr.getGCMD());
			if (lnrs != null) {
				lnrs.remove(lnr);
				if (lnrs.size() == 0) {
					listensByGCMD.remove(lnr.getGCMD());
				}
			}
		}
	}
}
