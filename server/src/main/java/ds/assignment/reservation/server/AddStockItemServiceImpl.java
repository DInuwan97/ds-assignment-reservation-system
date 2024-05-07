package ds.assignment.reservation.server;

import ds.assignment.reservation.grpc.generated.AddStockItemRequest;
import ds.assignment.reservation.grpc.generated.AddStockItemResponse;
import ds.assignment.reservation.grpc.generated.AddStockItemServiceGrpc;
import ds.assignment.reservation.synchonization.ditributed.lock.DistributedTxCoordinator;
import ds.assignment.reservation.synchonization.ditributed.lock.DistributedTxListener;
import ds.assignment.reservation.synchonization.ditributed.lock.DistributedTxParticipant;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.UUID;

public class AddStockItemServiceImpl extends AddStockItemServiceGrpc.AddStockItemServiceImplBase implements DistributedTxListener {

    private StockReservationServer server;
    private ManagedChannel channel = null;
    AddStockItemServiceGrpc.AddStockItemServiceBlockingStub clientStub = null;
    private AbstractMap.SimpleEntry<String, AddStockItemRequest> tempDataHolder;
    private boolean addItemTransactionStatus = false;
    private String tempItemName = null;
    private int tempItemQuantity = 0;

    public AddStockItemServiceImpl (StockReservationServer server) {
        this.server = server;
    }

    private void startDistributedTx(String itemId, AddStockItemRequest addStockItemRequest) {
        try {
            server.getAddStockItemTransaction().start(itemId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new AbstractMap.SimpleEntry<>(itemId, addStockItemRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AddStockItemResponse callServer(String itemName, int stockQuantity, double unitPrice, boolean isSentByPrimary, String IPAddress,
                                        int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = AddStockItemServiceGrpc.newBlockingStub(channel);

        AddStockItemRequest request = AddStockItemRequest
                .newBuilder()
                .setItemId(1)
                .setItemName(itemName)
                .setStockQuantity(stockQuantity)
                .setUnitPrice(unitPrice)
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        AddStockItemResponse response = clientStub.addStockItem(request);
        return response;
    }

    private AddStockItemResponse callPrimary(String itemName, int stockQuantity, double unitPrice) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(itemName, stockQuantity, unitPrice, false, IPAddress, port);
    }

    private void updateSecondaryServers(String itemName, int stockQuantity, double unitPrice)
            throws KeeperException, InterruptedException {
        System.out.println("Updating other servers");
        List<String[]> othersData = server.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(itemName, stockQuantity, unitPrice, true, IPAddress, port);
        }
    }

    @Override
    public void addStockItem(AddStockItemRequest request, StreamObserver<AddStockItemResponse> responseObserver) {

        String itemName = request.getItemName();
        int stockQuantity = request.getStockQuantity();
        double unitPrice = request.getUnitPrice();

        if(server.isLeader()) {
            try {
                System.out.println("Adding stock as Primary");
                startDistributedTx(itemName, request);
                updateSecondaryServers(itemName, stockQuantity, unitPrice);
                System.out.println("going to perform");
                if (stockQuantity > 0){
                    ((DistributedTxCoordinator)server.getAddStockItemTransaction()).perform();
                } else {
                    ((DistributedTxCoordinator)server.getAddStockItemTransaction()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while adding a new item" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (request.getIsSentByPrimary()) {
                System.out.println("Adding stock on a secondary, on Primary's command");
                startDistributedTx(itemName, request);
                if (stockQuantity != 0) {
                    ((DistributedTxParticipant)server.getAddStockItemTransaction()).voteCommit();
                } else {
                    ((DistributedTxParticipant)server.getAddStockItemTransaction()).voteAbort();
                }
            } else {
                AddStockItemResponse response = callPrimary(itemName, stockQuantity, unitPrice);
                if (response.getStatus()) {
                    addItemTransactionStatus = true;
                }
            }

            AddStockItemResponse response = AddStockItemResponse
                    .newBuilder()
                    .setStatus(addItemTransactionStatus)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    private void updateStock() {
        if (tempDataHolder != null) {
            AddStockItemRequest request = tempDataHolder.getValue();
            server.addStocks(request.getStockQuantity());
            System.out.println("Item " + request.getItemName() + " of type " + request.getType() + "Qty : " +request.getStockQuantity()+ " Added & committed");
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
