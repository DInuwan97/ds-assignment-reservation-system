package ds.assignment.reservation.synchonization.ditributed.lock;

public interface DistributedTxListener {
    void onGlobalCommit();
    void onGlobalAbort();
}
