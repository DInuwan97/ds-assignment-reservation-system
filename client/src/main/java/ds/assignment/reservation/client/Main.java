package ds.assignment.reservation.client;

import java.util.Scanner;

public class Main
{
    public static void main( String[] args ) throws InterruptedException
    {
        int port = 14336;
                //Integer.parseInt(args[0].trim());

            Scanner scanner = new Scanner(System.in);
            Scanner scanner1 = new Scanner(System.in);
            display();
            for (int i = 1; i <= 5; i++) {
                System.out.print("Enter your choice : ");
                int userChoice = scanner.nextInt();

                if (userChoice == 1 ){
                    AddStockItemServiceClient clientAdd = new AddStockItemServiceClient("localhost", port);
                    clientAdd .initializeConnection();
                    System.out.println("Enter the following details :");
                    System.out.println("ItemId ItemName Price Quantity Type :");
                    String userInput = scanner1.next();
                    clientAdd.processUserRequests(userInput.trim().split(","));
                    clientAdd.closeConnection();

                } else if (userChoice == 2 ){

                    GetStockItemServiceClient clientGet = new GetStockItemServiceClient("localhost", port);
                    clientGet.initializeConnection();
                    System.out.println("Enter the Item Name :");
                    String itemName = scanner1.nextLine();
                    clientGet.processUserRequests(itemName);
                    clientGet.closeConnection();

                } else if (userChoice == 5 ) {

                    ReserveStockItemServiceClient clientReserve = new ReserveStockItemServiceClient("",port);
                    clientReserve.initializeConnection();
                    System.out.println("Enter the following details :");
                    System.out.println("ItemName ReserveQuantity");
                    String input = scanner1.nextLine();
                    clientReserve.processUserRequests(input.trim().split(","));
                    clientReserve.closeConnection();
                }
            }
            scanner.close();
            scanner1.close();


        }

    public static void display(){
        System.out.println("1 - Add Item");
        System.out.println("2 - Get Item");
        System.out.println("3 - Update Item");
        System.out.println("4 - Delete Item");
        System.out.println("5 - Reserve Item");
        System.out.println("6 - Add User\n");
    }

}



