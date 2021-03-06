package org.project.model.dao.users;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.project.exceptions.UserAlreadyExistException;
import org.project.model.connection.ConnectionStrategy;
import org.project.model.dao.friends.RequestStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class UsersDAOImpl implements UsersDAO, ConnectionStrategy{

    ConnectionStrategy connectionStrategy;
    Connection connection;
    Logger logger = Logger.getLogger(UsersDAOImpl.class.getName());


    public UsersDAOImpl(ConnectionStrategy con) throws SQLException {
        this.connectionStrategy = con;
        connection = connectionStrategy.getConnection();
    }

    @Override
    public Users login(String phoneNumber) {
        Users user;
        ResultSet rs = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT id,name,phone_number,email,picture,password,gender,country,date_of_birth,bio,status FROM users WHERE phone_number=?" , ResultSet.CLOSE_CURSORS_AT_COMMIT);){
            ps.setString(1, phoneNumber);
            rs = ps.executeQuery();
            if (rs.next()) {
                user = extractUserFromResultSet(rs);
                getUserFriends(user);
                getUserNotifications(user);
                updateStatus(user , UserStatus.valueOf("Available"));
                return user;
            }

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }



    @Override
    public boolean register(Users user) throws UserAlreadyExistException {
        if (isUserExist(user.getPhoneNumber()))
            throw new UserAlreadyExistException("User Already exist in our DB");
        //Check first if name exist using isUserExistMethod then register
        String sql = "Insert into users (phone_number,name,email,password,country,picture)" +
                " values (?,?,?,?,?,?)";
        ByteArrayInputStream bais =null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setString(1, user.getPhoneNumber());
            preparedStatement.setString(2, user.getName());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, user.getPassword());
            preparedStatement.setString(5, user.getCountry());
            if (user.getDisplayPicture() != null) {
                bais = new ByteArrayInputStream(user.getDisplayPicture());
                preparedStatement.setBinaryStream(6, bais, user.getDisplayPicture().length);
            }else{
                URL url = getClass().getResource("/org/project/images/unknown.png");
                Path dest = Paths.get(url.toURI());
                if (dest != null) {
                    byte[] bytes = Files.readAllBytes(dest);
                    bais = new ByteArrayInputStream(bytes);
                    preparedStatement.setBinaryStream(6, bais, bytes.length);
                }
            }
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException | URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateUser(Users user) {
        // make sure no empty mandatory fields
        // make sure input is validated

        try {
            if(user.getDisplayPicture()!=null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(user.getDisplayPicture());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ResultSet rs = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT id,name,phone_number,email,picture,password,gender,country,date_of_birth,bio,status FROM users WHERE users.id=?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);) {
            ps.setInt(1, user.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                rs.updateString("phone_number", user.getPhoneNumber());
                rs.updateString("name", user.getName());
                rs.updateString("email", user.getEmail());
                rs.updateString("password", user.getPassword());
                rs.updateString("gender", String.valueOf(user.getGender()));
                rs.updateString("country", user.getCountry());
                rs.updateDate("date_of_birth", user.getDateOfBirth());
                rs.updateString("bio", user.getBio());
                rs.updateString("status", String.valueOf(user.getStatus()));
                rs.updateRow();
                if(user.getDisplayPicture()!=null) {
                    updatePicture(user);
                }
                return true;
            }

        } catch (SQLException e) {
            logger.warning(e.getSQLState());
            logger.warning(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        return false;
    }
    public void updatePicture(Users user){
        System.out.println("inside updtate pic");
        try {
            System.out.println("display picture is " + user.getDisplayPicture());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ByteArrayInputStream bais =null;
        PreparedStatement pstmt = null;
        int rowsAdded = 0;
        try {
            System.out.println("user picture is"+user.getDisplayPicture()+"");
            bais=new ByteArrayInputStream(user.getDisplayPicture());
            String SQL = "UPDATE users SET picture = ? WHERE  id= ?";
            pstmt = connection.prepareStatement(SQL);
            pstmt.setBinaryStream(1, bais, user.getDisplayPicture().length);
            System.out.println("user Id is  : " + user.getId() );
            pstmt.setInt(2,user.getId());
            rowsAdded = pstmt.executeUpdate();
            if (rowsAdded > 0)
                System.out.println("ay 7aga");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (rowsAdded > 0) {
            System.out.println("user was inserted successfully!");
        }
    }



    @Override
    public boolean deleteUSer(Users user) {
        ResultSet rs = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT id,name,phone_number,email,picture,password,gender,country,date_of_birth,bio,status FROM users WHERE users.phone_number=?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);) {
            ps.setString(1, user.getPhoneNumber());
            rs = ps.executeQuery();
            if (rs.next()) {
                rs.deleteRow();
                return true;
            }

        } catch (SQLException e) {
            logger.warning(e.getSQLState());
            logger.warning(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        return false;
    }

    @Override
    public ArrayList<Users> getUserFriends(Users user) {
        ResultSet rs = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT u.id, u.name , u.phone_number, u.status,u.picture" +
                " FROM users u JOIN friends f on f.friend_id=u.id" +
                " where f.user_id=? AND f.friend_status=?" +
                " union" +
                " SELECT u.id, u.name , u.phone_number, u.status,u.picture" +
                " FROM users u JOIN friends f on f.user_id=u.id" +
                " where f.friend_id=? AND f.friend_status=?;");) {
            ps.setInt(1, user.getId());
            ps.setString(2, String.valueOf(RequestStatus.Accepted));
            ps.setInt(3, user.getId());
            ps.setString(4, String.valueOf(RequestStatus.Accepted));
            rs = ps.executeQuery();
            user.getFriends().clear();
            while (rs.next()) {
                Users friend = extractFriendFromResultSet(rs);
                user.getFriends().add(friend);
            }
            return user.getFriends();

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ObservableList<Users> getUsers() {
        ObservableList<Users> users= FXCollections.observableArrayList();;
        ResultSet rs = null;
        try (PreparedStatement ps = connection.prepareStatement("select * FROM users ;")) {

            rs = ps.executeQuery();
            while (rs.next()) {
                Users user =  extractUserFromResultSet(rs);
                users.add(user);

                //friend.setFriend(extractFriendFromResultSet(rs));
            }

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return users;

    }

    @Override
    public ArrayList<Users> getUserNotifications(Users user) {
        ResultSet rs = null;
        try (PreparedStatement ps = connection.prepareStatement("select u.id, u.name , u.phone_number, u.status, u.picture FROM users u JOIN friends f on u.id=f.user_id where f.friend_id=? AND f.friend_status=? ;");) {
            ps.setInt(1, user.getId());
            ps.setString(2, String.valueOf(RequestStatus.Pending));
            rs = ps.executeQuery();
            user.getRequest_notifications().clear();
            while (rs.next()) {
                Users friend =  extractFriendFromResultSet(rs);
                user.getRequest_notifications().add(friend);
            }
            System.out.println("user notifications for"+user.getName()+":"+user.getRequest_notifications());
            return user.getRequest_notifications();

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    private Users extractUserFromResultSet(ResultSet rs) throws SQLException {
        Users user = new Users();
        user.setId(rs.getInt("id"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setGender(Gender.valueOf(rs.getString("gender")));
        user.setCountry(rs.getString("country"));
        user.setDateOfBirth(rs.getDate("date_of_birth"));
        user.setBio(rs.getString("bio"));
        user.setStatus(UserStatus.valueOf(rs.getString("status")));
        user.setDisplayPicture(rs.getBytes("picture"));
        return user;
    }

    private Users extractFriendFromResultSet(ResultSet rs) throws SQLException {
        Users user = new Users();
        user.setId(rs.getInt("id"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setName(rs.getString("name"));
        user.setStatus(UserStatus.valueOf(rs.getString("status")));
        user.setDisplayPicture(rs.getBytes("picture"));

        System.out.println("inside get frinds bytes"+user.getDisplayPicture());

        return user;
    }

    public boolean isUserExist(String phoneNumber) {
        try {
            PreparedStatement ps = connection.prepareStatement("select id,name from users where phone_number=?;");
            ps.setString(1, phoneNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return true;

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
        }
        return false;


    }

    @Override
    public boolean matchUserNameAndPassword(String phoneNumber, String password) {
        try {
            PreparedStatement ps = connection.prepareStatement("select id,name from users where phone_number=? And password=?");
            ps.setString(1, phoneNumber);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            }

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean updateStatus(Users user, UserStatus status) {
        System.out.println("update User");
        try (PreparedStatement ps = connection.prepareStatement("update users set Status =? where id = ?;")) {
            ps.setString(1, String.valueOf(status));
            ps.setInt(2, user.getId());
            if (ps.executeUpdate() > 0){
                System.out.println(user);
                return true;
            }
                return true;
        } catch (SQLException e) {
            logger.warning(e.getSQLState());
            logger.warning(e.getMessage());
            e.printStackTrace();
        }


        return false;
    }

    @Override
    public int getUserIDByPhoneNo(String phoneNo) {
        int userId = 0;
        ResultSet resultSet = null;
        if (isUserExist(phoneNo)) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT id from users where phone_number=?;")) {
                preparedStatement.setString(1, phoneNo);
                resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    userId = resultSet.getInt(1);
                }

            } catch (SQLException ex) {
                logger.warning(ex.getSQLState());
                logger.warning(ex.getMessage());
                ex.printStackTrace();
            }
        }
        return userId;
    }

    @Override
    public Map<String, Integer> getUsersNumByCountry() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        ResultSet resultSet = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT count(id) ,country from users group by(country);")) {
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                map.put(resultSet.getString(2), resultSet.getInt(1));
            }

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    @Override
    public Map<String, Integer> getUsersByGender() {
        Map<String, Integer> usersNumByGendermap = new HashMap<String, Integer>();
        ResultSet resultSet = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT count(id) ,gender from users group by(gender);")) {
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                usersNumByGendermap.put(resultSet.getString(2), resultSet.getInt(1));
            }

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return usersNumByGendermap;
    }

    @Override
    public Map<String, Integer> getUsersByStatus() {
        int counter =0;
        Map<String, Integer> usersNumByStatusmap = new HashMap<String, Integer>();
        ResultSet resultSet = null;
        try (PreparedStatement ps = connection.prepareStatement("select count(id) from users where status in('Away','Available','Busy') union select count(id)   from users where status ='Offline';")) {
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                if(counter == 0){
                    usersNumByStatusmap.put("Online", resultSet.getInt(1));
                    counter++;
                }else{
                    usersNumByStatusmap.put("Offline", resultSet.getInt(1));
                }

            }

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return usersNumByStatusmap;
    }

    @Override
    public boolean addContactRequest(List<String> contactList, Users user) {
        int friendId = 0;
        int result = 0;
        boolean added = false;
        if (contactList.size() > 0) {
            for (String phoneNo : contactList) {
                friendId = getUserIDByPhoneNo(phoneNo);
                String sql = "Insert into friends (friend_status,user_id,friend_id)" +
                        " values (?,?,?)";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                    preparedStatement.setString(1, "Pending");
                    preparedStatement.setInt(2, user.getId());
                    preparedStatement.setInt(3, friendId);
                    result = preparedStatement.executeUpdate();
                    if (result > 0) {
                        added = true;
                    }
                } catch (SQLException e) {
                    logger.warning(e.getSQLState());
                    logger.warning(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return added;
    }

    @Override
    public List<String> getUsersList(int userId) {
        List<String> usersList = new ArrayList<>();
        ResultSet resultSet = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement("select phone_number from users  where id != ALL ( select user_id from friends where friend_id = ?  And friend_status in('Accepted') union  select friend_id from friends where (user_id = ? ) And friend_status  in ('Accepted','Pending')) and id != ?",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, userId);
            preparedStatement.setInt(3, userId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                usersList.add(resultSet.getString(1));
            }

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return usersList;
    }

    @Override
    public boolean acceptRequest(Users currentUser, Users friend) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE friends SET friend_status='Accepted' WHERE user_id=? AND friend_id=?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);) {
            ps.setInt(1, friend.getId());
            ps.setInt(2,currentUser.getId());
            ps.executeUpdate();
            currentUser.setRequest_notifications(getUserNotifications(currentUser));
            currentUser.setFriends(getUserFriends(currentUser));
            System.out.println("done Accepted");

            return true;
        } catch (SQLException e) {
            logger.warning(e.getSQLState());
            logger.warning(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean declineRequest(Users currentUser, Users friend) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE from friends WHERE friend_status='Pending' AND user_id=? AND friend_id=?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);) {
            ps.setInt(1, friend.getId());
            ps.setInt(2,currentUser.getId());
            ps.executeUpdate();
            currentUser.setRequest_notifications(getUserNotifications(currentUser));
            System.out.println("done Declined");
            return true;
        } catch (SQLException e) {
            logger.warning(e.getSQLState());
            logger.warning(e.getMessage());
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public ArrayList<Users> getUserOnlineFriends(Users user) {
        ResultSet rs = null;
        ArrayList<Users> OnlineFriendsList = new ArrayList<Users>();
        try (PreparedStatement ps = connection.prepareStatement("select u2.* from friends fr, users u1, users u2 where" +
                " u1.id = fr.user_id and u2.status= 'Available'" +
                " and fr.friend_id = u2.id" +
                " and u2.id != u1.id and u1.id = ?" +
                " union" +
                " select u2.* from friends fr, users u1, users u2 where" +
                " u1.id = fr.friend_id and  u2.status= 'Available' and u2.id = fr.user_id" +
                " and u2.id != u1.id and u1.id = ?;")){
            ps.setInt(1, user.getId());
            ps.setInt( 2 , user.getId());
            rs = ps.executeQuery();
            while (rs.next())
            {
                OnlineFriendsList.add(extractUserFromResultSet(rs));

            }

        } catch (SQLException ex) {
            ex.printStackTrace();

        }
        return OnlineFriendsList;
    }


    @Override
    public Connection getConnection() throws SQLException {
            Connection conn;
            conn = connectionStrategy.getConnection();
            return conn;

    }
    @Override
    public ArrayList<Users> getAllOnlineUsers() {
        ArrayList<Users> onlineUserslist = new ArrayList<>();
        ResultSet resultSet = null;
        try (PreparedStatement ps = connection.prepareStatement("select id,phone_number,name,email,picture, password,gender,country,date_of_birth,bio,status FROM users  where status in ('Available','Busy','Away');")) {
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                onlineUserslist.add(extractUserFromResultSet(resultSet));
            }

        } catch (SQLException ex) {
            logger.warning(ex.getSQLState());
            logger.warning(ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return onlineUserslist;
    }



}
