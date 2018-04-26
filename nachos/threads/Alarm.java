package nachos.threads;

import nachos.machine.*;
import nachos.threads.Alarm.SleepThread;

import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		waitQueue = new PriorityQueue<SleepThread>();
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean intStatus = Machine.interrupt().disable();
		SleepThread t = waitQueue.peek();
		while(t!=null){
			if(Machine.timer().getTime()>=t.getWaitTime()){
				waitQueue.poll();
				t.ready();
			} else {
				break;
			}
			t = waitQueue.peek();
		}
		Machine.interrupt().restore(intStatus);
		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		if(x<=0){
			return;
		}
		boolean intStatus = Machine.interrupt().disable();
		waitQueue.add(new SleepThread(KThread.currentThread(), Machine.timer().getTime()+x));
		KThread.sleep();
		Machine.interrupt().restore(intStatus);
		
		
		// for now, cheat just to get something working (busy waiting is bad)
//		long wakeTime = Machine.timer().getTime() + x;
//		while (wakeTime > Machine.timer().getTime())
//			KThread.yield();
	}
	
	public static class SleepThread implements Comparable<SleepThread>{
		SleepThread(KThread t, long w){
			thread = t;
			waitTime = w;
		}
		
		public long getWaitTime(){
			return waitTime;
		}
		
		public void ready(){
			thread.ready();
		}

		@Override
		public int compareTo(SleepThread s) {
			// TODO Auto-generated method stub
			if(waitTime > s.getWaitTime()){
				return 1;
			} else if(waitTime < s.getWaitTime()){
				return -1;
			} else {
				return 0;
			}
		}
		
		private KThread thread;
		private long waitTime;
	}
	
   /**
    * Test alarm
    */
	private static class AlarmTest1 implements Runnable {
		int durations[] = {-1000, 0, 1000, 10*1000, 100*1000};
		long t0, t1;
		
		public AlarmTest1(int w){
			which = w;
			if(w==0){
				durations[0]=100;
				durations[2]=100*1000;
				durations[4]=1000;
			}
		}
		
		public void run(){
			for (int d : durations) {
				t0 = Machine.timer().getTime();
				ThreadedKernel.alarm.waitUntil (d);
				t1 = Machine.timer().getTime();
				System.out.println ("alarmTest1: thread " + which + " waited for " + (t1 - t0) + " ticks");
			}
		}
		
		private int which;
	}
	
	public static void selfTest(){
		new KThread(new AlarmTest1(1)).setName("alarm thread").fork();
		new AlarmTest1(0).run();
	}
	
	private PriorityQueue<SleepThread> waitQueue;
}
