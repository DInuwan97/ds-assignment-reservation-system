package ds.assignment.reservation.server;

import ds.assignment.reservation.grpc.generated.*;
import ds.assignment.reservation.synchonization.ditributed.lock.DistributedTxCoordinator;
import ds.assignment.reservation.synchonization.ditributed.lock.DistributedTxListener;
import ds.assignment.reservation.synchonization.ditributed.lock.DistributedTxParticipant;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.AbstractMap;
import java.util.List;
import java.util.UUID;

public class ReserveStockItemImpl extends ReserveItemServiceGrpc.ReserveItemServiceImplBase implements DistributedTxListener {
    private StockReservationServer server;
    private ManagedChannel channel = null;

    private AbstractMap.SimpleEntry<String, ReserveItemRequest> tempDataHolder;
    private boolean reserveItemTransactionStatus = false;

    ReserveItemServiceGrpc.ReserveItemServiceBlockingStub clientStub = null;

    public ReserveStockItemImpl(StockReservationServer server) {
        this.server = server;
    }

    private void startDistributedTx(String itemId, ReserveItemRequest reserveItemRequest) {
        try {
            server.getReserveStockItemTransaction().start(itemId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new AbstractMap.SimpleEntry<>(itemId, reserveItemRequest);
            System.out.println("Temp data updated.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void purchaseItem(ReserveItemRequest reserveItemRequest,
                             StreamObserver<ReserveItemResponse> responseObserver) {

        String itemName = reserveItemRequest.getItemName();
        int quantity = reserveItemRequest.getQuantity();

        if (server.isLeader()){
            try {
                System.out.println("Reserving item as Primary");
                startDistributedTx(itemName, reserveItemRequest);
                updateSecondaryServers(itemName, quantity);
                System.out.println("going to perform");
                if (quantity > 0){
                    ((DistributedTxCoordinator)server.getReserveStockItemTransaction()).perform();
                } else {
                    ((DistributedTxCoordinator)server.getReserveStockItemTransaction()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while reserving an item" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (reserveItemRequest.getIsSentByPrimary()) {
                System.out.println("Reserving stocks on item in a secondary, on Primary's command");
                startDistributedTx(itemName, reserveItemRequest);
                if (quantity != 0) {
                    ((DistributedTxParticipant)server.getReserveStockItemTransaction()).voteCommit();
                } else {
                    ((DistributedTxParticipant)server.getReserveStockItemTransaction()).voteAbort();
                }
            } else {
                ReserveItemResponse response = callPrimary(itemName, quantity);
                if (response.getStatus()) {
                    reserveItemTransactionStatus = true;
                }
            }
        }
    }

    private void updateSecondaryServers(String itemName, int reserveQty)
            throws KeeperException, InterruptedException {
        System.out.println("Updating other servers");
        List<String[]> othersData = server.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(itemName, reserveQty,true, IPAddress, port);
        }
    }

    private ReserveItemResponse callPrimary(String itemName, int stockQuantity) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(itemName, stockQuantity, false, IPAddress, port);
    }

    private ReserveItemResponse callServer(String itemName, int reserveQty, boolean isSentByPrimary, String IPAddress,
                                            int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = ReserveItemServiceGrpc.newBlockingStub(channel);

        ReserveItemRequest request = ReserveItemRequest
                .newBuilder()
                .setItemName(itemName)
                .setQuantity(reserveQty)
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        ReserveItemResponse response = clientStub.purchaseItem(request);
        return response;
    }

    private void updateStock() {
        if (tempDataHolder != null) {
            ReserveItemRequest request = tempDataHolder.getValue();
            server.reserveItem(request.getQuantity());
            System.out.println("Item " + request.getItemName() + " Qty : " +request.getQuantity()+ "committed");
            tempDataHolder = null;
        }
    }

    @Override
    public void onGlobalCommit() {
        updateStock();
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        System.out.println("Transaction Aborted by the Coordinator");
    }
}
