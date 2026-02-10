package protocoltests.protocol.messages;

import java.util.Set;

public record ListUsersResp (Set<String> userList, String status){
}
