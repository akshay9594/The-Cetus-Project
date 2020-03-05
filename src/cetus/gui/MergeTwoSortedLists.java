package cetus.gui;

//Definition for singly-linked list.
class ListNode {
	int val;
	ListNode next;

	ListNode(int x) {
		val = x;
		next = null;
	}
}

public class MergeTwoSortedLists {

	public ListNode mergeTwoLists(ListNode l1, ListNode l2) {
		// Start typing your Java solution below
		// DO NOT write main() function
		if (l1 == null)
			return l2;
		if (l2 == null)
			return l1;

		ListNode a = l1;
		ListNode b = l2;
		ListNode head = new ListNode(0);
		ListNode c = head;

		while (a != null && b != null) {
//			System.out.println(a.val);
//			System.out.println(b.val);

			if (a.val < b.val) {
				c.next = new ListNode(a.val);
				a = a.next;
			} else {
				c.next = new ListNode(b.val);
				b = b.next;
			}
			c = c.next;
		}
		
		while (a != null) {
			c.next = new ListNode(a.val);
			a = a.next;
			c = c.next;
		}
		
		while (b != null) {
			c.next = new ListNode(b.val);
			b = b.next;
			c = c.next;
		}
		
		return head.next;
	}
	

	public void printList(ListNode a) {
		while (a != null) {
			System.out.println(a.val);
			a = a.next;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ListNode a = new ListNode(5);
		ListNode b = new ListNode(1);
		b.next = new ListNode(2);
		b.next.next = new ListNode(4);

		MergeTwoSortedLists x = new MergeTwoSortedLists();
		x.printList(a);
		x.printList(b);

		ListNode c = x.mergeTwoLists(a, b);
		x.printList(c);

	}

}
