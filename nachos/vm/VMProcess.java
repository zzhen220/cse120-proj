package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		fileTable = new OpenFile[fileTableSize];
		numFiles = 2;
		fileTable[0] = UserKernel.console.openForReading();
		fileTable[1] = UserKernel.console.openForWriting();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		super.restoreState();
	}
	
	@Override
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		if (vaddr < 0 || vaddr >= numPages*pageSize)
			return 0;
		
		int vpn = vaddr/pageSize;
		int off = vaddr%pageSize;
		VMKernel.PINlock.acquire();
		pageTable[vpn].used = false;
		VMKernel.PINlock.release();
		if(!pageTable[vpn].valid)
			this.pageFaultHandler(vpn*pageSize);
		int paddr = pageTable[vpn].ppn * pageSize + off;
		int amount = Math.min(length, numPages*pageSize - vaddr);
		int readByte = Math.min(amount, pageSize - off);
		System.arraycopy(memory, paddr, data, offset, readByte);
		pageTable[vpn].used = true;
		offset += readByte;
		amount -= readByte;
		vpn += 1;
		while(amount > 0){
			pageTable[vpn].used = false;
			if(!pageTable[vpn].valid)
				this.pageFaultHandler(vpn*pageSize);
			paddr = pageTable[vpn].ppn * pageSize;
			readByte = Math.min(amount, pageSize);
			System.arraycopy(memory, paddr, data, offset, readByte);
			pageTable[vpn].used = true;
			offset += readByte;
			amount -= readByte;
			vpn += 1;
		}
		
		return offset;
	}

	@Override
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= numPages*pageSize)
			return 0;

		int vpn = vaddr/pageSize;
		int off = vaddr%pageSize;
		if(pageTable[vpn].readOnly)
			return 0;
		VMKernel.PINlock.acquire();
		pageTable[vpn].used = false;
		VMKernel.PINlock.release();
		if(!pageTable[vpn].valid)
			this.pageFaultHandler(vpn*pageSize);
		
		int paddr = pageTable[vpn].ppn * pageSize + off;
		int amount = Math.min(length, numPages*pageSize - vaddr);
		int writeByte = Math.min(amount, pageSize - off);
		System.arraycopy(data, offset, memory, paddr, writeByte);
		pageTable[vpn].used = true;
		offset += writeByte;
		amount -= writeByte;
		vpn += 1;
		while(amount > 0){
			pageTable[vpn].used = false;
			if(!pageTable[vpn].valid)
				this.pageFaultHandler(vpn*pageSize);
			paddr = pageTable[vpn].ppn * pageSize;
			writeByte = Math.min(amount, pageSize);
			System.arraycopy(data, offset, memory, paddr, writeByte);
			pageTable[vpn].used = true;
			offset += writeByte;
			amount -= writeByte;
			vpn += 1;
		}
		
		return offset;
	}
	
	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
//		VMKernel.FPTlock.acquire();
//		if(numPages > VMKernel.freePageTable.size()){
//			coff.close();
//			Lib.debug(dbgVM, "\tinsufficient physical memory");
//			VMKernel.FPTlock.release();
//			return false;
//		} else {
//			for(int i = 0; i < numPages; ++i){
//				int ppn = VMKernel.freePageTable.remove(0);
//				pageTable[i] = new TranslationEntry(i, ppn, false, false, false, false);
//			}
//		}
//		VMKernel.FPTlock.release();
		
		pageTable = new TranslationEntry[numPages];
		for(int i = 0; i < numPages; ++i){
			pageTable[i] = new TranslationEntry(i, 0, false, false, false, false);
		}

		
		return true;
		
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		for(int i = 0; i < pageTable.length; ++i){
			VMKernel.PRlock.acquire();
			if(pageTable[i].valid){
				pageTable[i].valid = false;
				VMKernel.freePageTable.add(0, pageTable[i].ppn);
				VMKernel.hand = VMKernel.clockTable.listIterator(VMKernel.clockTable.indexOf(pageTable[i]));
				VMKernel.hand.next();
				VMKernel.hand.remove();
			} else {
				if(pageTable[i].dirty){
					VMKernel.freeSwapPageTable.add(pageTable[i].vpn);
				}
			}
			VMKernel.PRlock.release();
		}
	}

	/**
	 * Handle page fault exception
	 * @param vaddr the virtual address caused page fault.
	 */
	protected void pageFaultHandler(int vaddr) {
		int vpn = vaddr/pageSize;
		VMKernel.PRlock.acquire();
		if(VMKernel.freePageTable.isEmpty()) {
			VMKernel.pageReplace(pageTable[vpn]);
		} else {
			pageTable[vpn].ppn = VMKernel.freePageTable.remove(0);
			VMKernel.hand.add(pageTable[vpn]);
		}
		pageTable[vpn].valid = true;
		VMKernel.PRlock.release();
		if (pageTable[vpn].dirty){
			Lib.debug(dbgVM, "\tread from swap file");
			byte[] buffer = new byte[pageSize];
			VMKernel.swapFile.read(pageTable[vpn].vpn, buffer, 0, pageSize);
			VMKernel.freeSwapPageTable.add(pageTable[vpn].vpn);
			this.writeVirtualMemory(vpn*pageSize, buffer);
		} else {
			if(vpn >= numPages-1-stackPages) {
				Lib.debug(dbgVM, "\tvpn is not coff sections");
				byte[] zero = {0};
				this.writeVirtualMemory(vpn*pageSize, zero);
			} else {
				for (int s = 0; s < coff.getNumSections(); s++) {
					CoffSection section = coff.getSection(s);
					if(vpn >= section.getFirstVPN() && vpn < section.getFirstVPN()+section.getLength()) {
						Lib.debug(dbgVM, "\tvpn is " + section.getName());
						pageTable[vpn].readOnly = section.isReadOnly();
						section.loadPage(vpn-section.getFirstVPN(), pageTable[vpn].ppn);
						break;
					}
				}
			}
		}
	}
	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exception</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionPageFault:
			this.pageFaultHandler(processor.readRegister(Processor.regBadVAddr));
			break;
		default:
			super.handleException(cause);
			break;
		}
	}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
