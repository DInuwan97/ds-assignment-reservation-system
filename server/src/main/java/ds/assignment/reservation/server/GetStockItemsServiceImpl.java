package ds.assignment.reservation.server;

import ds.assignment.reservation.grpc.generated.GetStockItemRequest;
import ds.assignment.reservation.grpc.generated.GetStockItemResponse;
import ds.assignment.reservation.grpc.generated.GetStockItemServiceGrpc;
import io.grpc.stub.StreamObserver;

public class GetStockItemsServiceImpl
        extends GetStockItemServiceGrpc.GetStockItemServiceImplBase {

    private StockReservationServer server;

    public GetStockItemsServiceImpl(StockReservationServer server) {
        this.server = server;
    }

    @Override
    public void getStockItem(GetStockItemRequest request, StreamObserver<GetStockItemResponse> responseObserver) {

        String itemName = request.getItemName();
        int stock = getItemStock(itemName);
        GetStockItemResponse response = GetStockItemResponse
                .newBuilder()
                .setItemName(request.getItemName())
                .build();
        System.out.println("GetStockItemServiceImpl: getStock: " + itemName + " " + stock);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private int getItemStock(String itemName){
        return server.getStock();
    }

}