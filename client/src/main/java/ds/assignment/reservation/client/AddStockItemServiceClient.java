package ds.assignment.reservation.client;

import ds.assignment.reservation.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class AddStockItemServiceClient {
    private ManagedChannel channel = null;
    AddStockItemServiceGrpc.AddStockItemServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public AddStockItemServiceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection () {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        clientStub = AddStockItemServiceGrpc.newBlockingStub(channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(String[] input) throws InterruptedException {
        while (true) {
            AddStockItemRequest request = AddStockItemRequest
                    .newBuilder()
                    .setItemId(Integer.parseInt(input[0]))
                    .setItemName(input[1])
                    .setUnitPrice(Double.parseDouble(input[2]))
                    .setStockQuantity(Integer.parseInt(input[3]))
                    .setType(getType(input[4]))
                    .setIsSentByPrimary(false)
                    .build();
            AddStockItemResponse response = clientStub.addStockItem(request);
            System.out.println(input[1] +" has added");
            Thread.sleep(1000);
        }
    }

    private Type getType(String type) {
        if (type.equalsIgnoreCase("NEW_ARRIVAL")) {
            return Type.NEW_ARRIVAL;
        } else if (type.equalsIgnoreCase("SELL")) {
            return Type.SELL;
        } else {
            return Type.RENT;
        }
    }

}

