package ds.assignment.reservation.client;

import ds.assignment.reservation.grpc.generated.GetStockItemRequest;
import ds.assignment.reservation.grpc.generated.GetStockItemResponse;
import ds.assignment.reservation.grpc.generated.GetStockItemServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GetStockItemServiceClient {
    private ManagedChannel channel = null;

    GetStockItemServiceGrpc.GetStockItemServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public GetStockItemServiceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection () {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        clientStub = GetStockItemServiceGrpc.newBlockingStub(channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(String itemName) throws InterruptedException {
            System.out.println("Requesting server to check the available stocks for " + itemName);
            GetStockItemRequest request = GetStockItemRequest
                    .newBuilder()
                    .setItemName(itemName)
                    .build();
            GetStockItemResponse response = clientStub.getStockItem(request);
            System.out.printf(itemName + " has " + response.getStockQuantity() + " units in stock\n");
            Thread.sleep(1000);
    }
}
