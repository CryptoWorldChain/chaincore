package org.brewchain.account.test;

import org.brewchain.account.core.AccountHelper;

public class ThreadAccount extends Thread {

	private AccountHelper accountHelper;

	public ThreadAccount(AccountHelper accountHelper) {
		accountHelper = accountHelper;
	}

	@Override
	public void run() {

	}

}
