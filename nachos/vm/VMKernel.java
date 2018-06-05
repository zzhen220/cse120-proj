package nachos.vm;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		
		clockTable = new LinkedList<>();
		hand = clockTable.listIterator();
		freeSwapPageTable = new LinkedList<>();
		PRlock = new Lock();
		PINlock = new Lock();
		
		swapFile = fileSystem.open(swapFilename, true);
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		swapFile.close();
		fileSystem.remove(swapFilename);
		super.terminate();
	}
	
	public static void pageReplace(TranslationEntry after) {
		if(!hand.hasNext()){
			hand = clockTable.listIterator();
		}
		TranslationEntry before;
		VMKernel.PINlock.acquire();
		while(!(before = hand.next()).used) {
			if(!hand.hasNext()){
				hand = clockTable.listIterator();
			}
		}
		before.valid = false;
		VMKernel.PINlock.release();
		after.ppn = before.ppn;
		hand.set(after);
		if(before.dirty){
			Lib.debug(dbgVM, "\twrite to swap file");
			byte[] buffer = new byte[Processor.pageSize];
			System.arraycopy(Machine.processor().getMemory(), before.ppn*Processor.pageSize,
					buffer, 0, Processor.pageSize);
			if(freeSwapPageTable.isEmpty()){
				before.vpn = swapFile.tell();
				swapFile.write(buffer, 0, buffer.length);
			} else {
				int spn = freeSwapPageTable.remove(0);
				before.vpn = spn;
				swapFile.write(spn, buffer, 0, buffer.length);
			}
		}
	}

	protected static List<TranslationEntry> clockTable;
	
	protected static ListIterator<TranslationEntry> hand;
	
	private static final String swapFilename = "VMKernel_Swap.swap";
	
	protected static Lock PRlock;
	
	protected static Lock PINlock;
	
	protected static List<Integer> freeSwapPageTable;
	
	protected static OpenFile swapFile;
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';
}
