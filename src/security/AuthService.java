package security;
import java.util.*;

public class AuthService {
    private final Map<String, Role> users = new HashMap<>();
    public void register(String userId, Role role){ users.put(userId, role); }
    public boolean authorize(String userId, Role required){
        Role r = users.get(userId);
        if(r == null) return false;
        // simple rule: ADMIN can do everything, Manager >= Teller >= Customer
        if(r == Role.ADMIN) return true;
        if(required == Role.CUSTOMER) return true;
        if(required == Role.TELLER) return r == Role.TELLER || r == Role.MANAGER;
        if(required == Role.MANAGER) return r == Role.MANAGER;
        return false;
    }
}