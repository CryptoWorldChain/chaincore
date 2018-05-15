package org.brewchain.account.doublyll;

public class Node {
	public byte[] data;
	public int num;
	public Node next;
	public Node prev;

	public Node(byte[] v, int number) {
		data = v;
		num = number;
		next = null;
		prev = null;
	}
}