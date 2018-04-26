package nachos.threads;

import nachos.machine.*;

/**
 * A <i>GameMatch</i> groups together player threads of the same
 * ability into fixed-sized groups to play matches with each other.
 * Implement the class <i>GameMatch</i> using <i>Lock</i> and
 * <i>Condition</i> to synchronize player threads into groups.
 */
public class GameMatch {
    
    /* Three levels of player ability. */
    public static final int abilityBeginner = 1,
	abilityIntermediate = 2,
	abilityExpert = 3;

    /**
     * Allocate a new GameMatch specifying the number of player
     * threads of the same ability required to form a match.  Your
     * implementation may assume this number is always greater than zero.
     */
    public GameMatch (int numPlayersInMatch) {
    	total = numPlayersInMatch;
    	lock = new Lock[3];
    	cv = new Condition[3];
    	result = new int[3];
    	for(int i=0; i < 3; i++){
    		lock[i] = new Lock();
    		cv[i] = new Condition(lock[i]);
    	}
    }

    /**
     * Wait for the required number of player threads of the same
     * ability to form a game match, and only return when a game match
     * is formed.  Many matches may be formed over time, but any one
     * player thread can be assigned to only one match.
     *
     * Returns the match number of the formed match.  The first match
     * returned has match number 1, and every subsequent match
     * increments the match number by one, independent of ability.  No
     * two matches should have the same match number, match numbers
     * should be strictly monotonically increasing, and there should
     * be no gaps between match numbers.
     * 
     * @param ability should be one of abilityBeginner, abilityIntermediate,
     * or abilityExpert; return -1 otherwise.
     */
    public int play (int ability) {
    	if(ability!=abilityBeginner&&ability!=abilityIntermediate&&ability!=abilityExpert)
    		return -1;
    	ability--;
    	lock[ability].acquire();
    	if(++numc[ability] < total){
    		while(!(numo[ability]>0)){
    			cv[ability].sleep();
    		}
    		numo[ability]--;
    		lock[ability].release();
    		return result[ability];
    	} else {
    		cv[ability].wakeAll();
    		numc[ability] = 0;
    		numo[ability] = total-1;
    		lock[ability].release();
    		result[ability] = ++matchNum;
    		return result[ability];
    	}
    }
    
    public static void matchTest4 () {
    	final GameMatch match = new GameMatch(2);

    	// Instantiate the threads
    	KThread beg1 = new KThread( new Runnable () {
    		public void run() {
    		    int r = match.play(GameMatch.abilityBeginner);
    		    System.out.println ("beg1 matched for " + r);
    		    // beginners should match with a match number of 1
    		    //Lib.assertTrue(r == 1, "expected match number of 1");
    		}
    	});
    	beg1.setName("B1");

    	KThread beg2 = new KThread( new Runnable () {
    		public void run() {
    		    int r = match.play(GameMatch.abilityBeginner);
    		    System.out.println ("beg2 matched for " + r);
    		    // beginners should match with a match number of 1
    		    //Lib.assertTrue(r == 1, "expected match number of 1");
    		}
    	});
    	beg2.setName("B2");
    	
    	KThread beg3 = new KThread( new Runnable () {
    		public void run() {
    		    int r = match.play(GameMatch.abilityBeginner);
    		    System.out.println ("beg3 matched for " + r);
    		    // beginners should match with a match number of 1
    		    //Lib.assertTrue(r == 2, "expected match number of 1");
    		}
    	});
    	beg3.setName("B3");

    	KThread int1 = new KThread( new Runnable () {
    		public void run() {
    		    int r = match.play(GameMatch.abilityIntermediate);
    		    System.out.println("int1 matched for " + r);
    		    //Lib.assertNotReached("int1 should not have matched!");
    		}
    	});
    	
    	KThread beg4 = new KThread( new Runnable () {
    		public void run() {
    		    int r = match.play(GameMatch.abilityBeginner);
    		    System.out.println ("beg4 matched for " + r);
    		    // beginners should match with a match number of 1
    		    //Lib.assertTrue(r == 2, "expected match number of 1");
    		}
    	});
    	beg4.setName("B4");
    	
    	int1.setName("I1");

    	KThread exp1 = new KThread( new Runnable () {
    		public void run() {
    		    int r = match.play(GameMatch.abilityIntermediate);
    		    System.out.println("exp1 matched for " + r);
    		    //Lib.assertNotReached("exp1 should not have matched!");
    		}
    	});
    	exp1.setName("E1");

    	// Run the threads.  The beginner threads should successfully
    	// form a match, the other threads should not.  The outcome
    	// should be the same independent of the order in which threads
    	// are forked.
    	beg1.fork();
    	int1.fork();
    	beg2.fork();
    	beg3.fork();
    	beg4.fork();
    	exp1.fork();

    	// Assume join is not implemented, use yield to allow other
    	// threads to run
    	for (int i = 0; i < 10; i++) {
    	    KThread.yield();
    	}
    }
        
    public static void selfTest() {
    	matchTest4();
    }
    
    
    private int matchNum = 0;
    private int result[];
    private Lock lock[];
    private Condition cv[];
    private int total;
    private int numc[]={0,0,0};
    private int numo[]={0,0,0};
}
