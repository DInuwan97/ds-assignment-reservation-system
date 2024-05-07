package ds.assignment.reservation.client;

import ds.assignment.reservation.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ReserveStockItemServiceClient {

    private ManagedChannel channel = null;
    ReserveItemServiceGrpc.ReserveItemServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public ReserveStockItemServiceClient (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection () {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        clientStub = ReserveItemServiceGrpc.newBlockingStub(channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }


    public void processUserRequests(String[] input) throws InterruptedException {
        while (true) {
            ReserveItemRequest request = ReserveItemRequest
                    .newBuilder()
                    .setItemName(input[0])
                    .setQuantity(Integer.parseInt(input[1]))
                    .setIsSentByPrimary(false)
                    .build();
            ReserveItemResponse response = clientStub.purchaseItem(request);
            System.out.println("Qty:" +input[1] +" has reserved");
            Thread.sleep(1000);
        }
    }
}
