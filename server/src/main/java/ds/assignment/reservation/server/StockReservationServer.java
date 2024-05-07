package ds.assignment.reservation.server;

import ds.assignment.reservation.synchonization.ditributed.lock.DistributedLock;
import ds.assignment.reservation.synchonization.ditributed.lock.DistributedTx;
import ds.assignment.reservation.synchonization.ditributed.lock.DistributedTxCoordinator;
import ds.assignment.reservation.synchonization.ditributed.lock.DistributedTxParticipant;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class StockReservationServer {

    private DistributedLock leaderLock;
    private final int serverPort;
    private AtomicBoolean isLeader = new AtomicBoolean(false);
    private byte[] leaderData;
    private DistributedTx transactionItemAdd;
    private DistributedTx transactionItemReserve;
    private int stock = 0;


    AddStockItemServiceImpl addStockItemService;
    GetStockItemsServiceImpl getStockItemsService;
    ReserveStockItemImpl reserveStockItemService;


    public StockReservationServer(String host, int port) throws InterruptedException, IOException, KeeperException {
        this.serverPort = port;
        leaderLock = new DistributedLock("StockReservationServerCluster", buildServerData(host, port));
        addStockItemService = new AddStockItemServiceImpl(this);
        getStockItemsService = new GetStockItemsServiceImpl(this);
        reserveStockItemService = new ReserveStockItemImpl(this);
        transactionItemAdd = new DistributedTxParticipant(addStockItemService);
        transactionItemReserve = new DistributedTxParticipant(reserveStockItemService);
    }

    private void tryToBeLeader() throws KeeperException, InterruptedException {
        Thread leaderCampaignThread = new Thread(new LeaderCampaignThread());
        leaderCampaignThread.start();
    }

    public void startServer() throws IOException, InterruptedException, KeeperException {
        Server server = ServerBuilder
                .forPort(serverPort)
                .addService(addStockItemService)
                .addService(getStockItemsService)
                .addService(reserveStockItemService)
                .build();
        server.start();
        System.out.println("Server Started and ready to accept requests on port " + serverPort);

        tryToBeLeader();
        server.awaitTermination();
    }

    public static String buildServerData(String IP, int port) {
        StringBuilder builder = new StringBuilder();
        builder.append(IP).append(":").append(port);
        return builder.toString();
    }

    class LeaderCampaignThread implements Runnable {
        private byte[] currentLeaderData = null;

        @Override
        public void run() {
            System.out.println("Starting the leader Campaign");
            try {
                boolean leader = leaderLock.tryAcquireLock();
                while (!leader) {
                    byte[] leaderData = leaderLock.getLockHolderData();
                    if (currentLeaderData != leaderData) {
                        currentLeaderData = leaderData;
                        setCurrentLeaderData(currentLeaderData);
                    }
                    Thread.sleep(10000);
                    leader = leaderLock.tryAcquireLock();
                }
                beTheLeader();
                currentLeaderData = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void beTheLeader() {
        System.out.println("I got the leader lock. Now acting as primary");
        isLeader.set(true);
        transactionItemAdd = new DistributedTxCoordinator(addStockItemService);
        transactionItemReserve = new DistributedTxCoordinator(reserveStockItemService);
    }

    public boolean isLeader() {
        return isLeader.get();
    }

    private synchronized void setCurrentLeaderData(byte[] leaderData) {
        this.leaderData = leaderData;
    }
    public DistributedTx getAddStockItemTransaction() {
        return transactionItemAdd;
    }

    public DistributedTx getReserveStockItemTransaction() {
        return transactionItemReserve;
    }

    public synchronized String[] getCurrentLeaderData() {
        return new String(leaderData).split(":");
    }

    public List<String[]> getOthersData() throws KeeperException, InterruptedException {
        List<String[]> result = new ArrayList<>();
        List<byte[]> othersData = leaderLock.getOthersData();

        for (byte[] data : othersData) {
            String[] dataStrings = new String(data).split(":");
            result.add(dataStrings);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage executable-name <port>");
        }

        int serverPort = Integer.parseInt(args[0]);
        DistributedLock.setZooKeeperURL("localhost:2181");
        DistributedTx.setZooKeeperURL("localhost:2181");

        StockReservationServer server = new StockReservationServer("localhost", serverPort);
        server.startServer();
    }

    public void addStocks(int quantity) {
        stock += quantity;
    }
    public int getStock() {
        return stock;
    }

    public void reserveItem(int quantity) {
        stock -= quantity;
    }

}
