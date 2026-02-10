package Messages;

import java.util.Set;

public record UserListMessage (Set<String> userList, String status){
}
