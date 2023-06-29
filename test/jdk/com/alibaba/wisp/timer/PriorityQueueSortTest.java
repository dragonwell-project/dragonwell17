/*
 * @test
 * @library /lib/testlibrary
 * @summary Test TimeOut.Queue's offer and remove function, make sure it's consistent with the behavior of the jdk's priority queue
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm PriorityQueueSortTest
 */

import com.alibaba.wisp.engine.TimeOut;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;


import static jdk.testlibrary.Asserts.assertTrue;

public class PriorityQueueSortTest {

	static Class<?> classQ;
	static Class<?> classT;

	static Method poll = null;
	static Method offer = null;

	static TimeOut ILLEGAL_FLAG = new TimeOut(null, 1, false);

	static class MyComparator implements Comparator<TimeOut> {
		public int compare(TimeOut x, TimeOut y) {
			return Long.compare(getDeadNano(x), getDeadNano(y));
		}
	}

	static Long getDeadNano(TimeOut t) {
		try {
			Field deadline = TimeOut.class.getDeclaredField("deadlineNano");
			deadline.setAccessible(true);
			return (Long) deadline.get(t);
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	static Object getTimerQueue() {
		try {
			classQ = Class.forName("com.alibaba.wisp.engine.TimeOut$TimerManager$Queue");
			classT = Class.forName("com.alibaba.wisp.engine.TimeOut$TimerManager");
			Constructor c1 = classQ.getDeclaredConstructor(classT);
			c1.setAccessible(true);
			Constructor c2 = classT.getDeclaredConstructor();
			c2.setAccessible(true);
			poll = classQ.getDeclaredMethod("poll");
			poll.setAccessible(true);
			offer = classQ.getDeclaredMethod("offer", TimeOut.class);
			offer.setAccessible(true);
			return c1.newInstance(c2.newInstance());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	static boolean add(Object pq, TimeOut timeOut) {
		try {
			offer.invoke(pq, timeOut);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	static TimeOut poll(Object pq) {
		try {
			return (TimeOut)poll.invoke(pq);
		} catch (Exception e) {
			e.printStackTrace();
			return ILLEGAL_FLAG;
		}
	}

	public static void main(String[] args) {
		int n = 10000;

		List<Long> sorted = new ArrayList<>(n);
		for (int i = 0; i < n; i++)
			sorted.add(new Long(i));
		List<Long> shuffled = new ArrayList<>(sorted);
		Collections.shuffle(shuffled);

		Object pq = getTimerQueue();


		for (Iterator<Long> i = shuffled.iterator(); i.hasNext(); )
			add(pq, new TimeOut(null, i.next(), false));

		List<Long> recons = new ArrayList<>();

		while (true) {
			TimeOut t = poll(pq);
			if (t == null) {
				break;
			}
			recons.add(getDeadNano(t));
		}
		assertTrue(recons.equals(sorted), "Sort failed");

		for (Long val : recons) {
			add(pq, new TimeOut(null, val, false));
		}
		recons.clear();
		while(true){
			TimeOut timeOut = poll(pq);
			if (timeOut == null) {
				break;
			}
			if(getDeadNano(timeOut)  % 2 == 1) {
				recons.add(getDeadNano(timeOut));
			}
		}

		Collections.sort(recons);

		for (Iterator<Long> i = sorted.iterator(); i.hasNext(); )
			if ((i.next().intValue() % 2) != 1)
				i.remove();

		assertTrue(recons.equals(sorted), "Odd Sort failed");
	}
}
