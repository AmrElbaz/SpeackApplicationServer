package org.project.controller;

import org.project.controller.messages.Message;
import org.project.model.ChatRoom;
import org.project.model.connection.MysqlConnection;
import org.project.model.dao.users.Users;
import org.project.model.dao.users.UsersDAO;
import org.project.model.dao.users.UsersDAOImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class ServicesImp extends UnicastRemoteObject implements ServicesInterface {
    UsersDAO DAO;
    CopyOnWriteArrayList<ClientInterface> clients;
    CopyOnWriteArrayList<ChatRoom> chatRooms;

    public ServicesImp() throws RemoteException {
        super(1260);
        try {
                        DAO = new UsersDAOImpl(MysqlConnection.getInstance());
            clients = new CopyOnWriteArrayList<>();
            chatRooms = new CopyOnWriteArrayList<>();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Users getUserData(String phoneNumber) throws RemoteException {
        return DAO.login(phoneNumber);
    }

    @Override
    public Boolean register(Users user) throws RemoteException, SQLException {
        System.out.println(user + " is registered");
        return DAO.register(user);
    }

    @Override
    public Boolean checkUserLogin(String phoneNumber, String password) throws RemoteException {
        return DAO.matchUserNameAndPassword(phoneNumber, password);
    }

    @Override
    public ArrayList<Users> getFriends(String phoneNumber) throws RemoteException {
        return null;
    }

    @Override
    public ArrayList<Users> getNotifications(String phoneNumber) throws RemoteException {
        return null;
    }

    @Override
    public void notifyUpdate(Users users) throws RemoteException {

    }
@Override
    public void notifyUser(Message newMsg, ChatRoom chatRoom) throws RemoteException{
    chatRoom.getUsers().forEach(user -> {
        clients.forEach(clientInterface -> {
            try {
                if (clientInterface.getUser().getId() == user.getId()) {
                    System.out.println("sending file to " + clientInterface.getUser());
                    clientInterface.recieveFile(newMsg , chatRoom);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    });



    }

    @Override
    public void sendMessage(Message newMsg, ChatRoom chatRoom) throws RemoteException {
        chatRoom.getUsers().forEach(user -> {
            clients.forEach(clientInterface -> {
                try {
                    if (clientInterface.getUser().getId() == user.getId()) {
                        System.out.println("sending message to " + clientInterface.getUser());
                        clientInterface.recieveMsg(newMsg , chatRoom);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @Override
    public void registerClient(ClientInterface clientImp) throws RemoteException {
        System.out.println("in server register client");
        clients.add(clientImp);
        System.out.println("new Client is assigned" + clientImp.getUser());
    }

    @Override
    public ChatRoom requestChatRoom(ArrayList<Users> chatroomUsers) throws RemoteException {
        System.out.println(chatroomUsers.get(1).getChatRooms() + "  aloooooooo");
        ChatRoom chatRoomExist = checkChatRoomExist(chatroomUsers.get(1));
        if (chatRoomExist != null){
            System.out.println("-------------------------------------------------------------------");
            System.out.println("chat room Already Exist");
            System.out.println("-------------------------------------------------------------------");
            return chatRoomExist;
        }
        chatRoomExist = new ChatRoom();
        chatRoomExist.setChatRoomId(chatroomUsers.toString());
        chatRoomExist.setUsers(chatroomUsers);
        chatRooms.add(chatRoomExist);
        addChatRoomToAllClients(chatroomUsers , chatRoomExist);
        return chatRoomExist;
    }

    private void addChatRoomToAllClients(ArrayList<Users> chatroomUsers, ChatRoom chatRoomExist) {
        chatroomUsers.forEach(users -> {
            try {
                getClient(users).addChatRoom(chatRoomExist);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    private ChatRoom checkChatRoomExist(Users chatroomUser) {
        long count = 0;
        for (ChatRoom chatRoom : chatRooms) {
            System.out.println("IN THE LOOP");
            System.out.println(chatroomUser.getChatRooms());
            if (chatroomUser.getChatRooms().size() > 0){
                count = chatroomUser.getChatRooms().parallelStream().map(ChatRoom::getChatRoomId).filter(s -> s.equals(chatRoom.getChatRoomId())).count();
                System.out.println("count " + chatRoom.getChatRoomId());
                if (count > 0) {
                    System.out.println("chatRoom Already exists");
                    return chatRoom;}
            }

        }
        System.out.println(" rg3 b 5ofy 7onin");
        return null;
    }

    public ClientInterface getClient(Users user) {
        for (ClientInterface clientInterface : clients) {
            try {
                if (clientInterface.getUser().getId() == user.getId()) {
                    return clientInterface;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        System.out.println("this user has no life " + user);
        return null;
    }
}
