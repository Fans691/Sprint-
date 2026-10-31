package mg.itu.framework.util;

import java.util.ArrayList;
import java.util.List;

public class AuthUtil {

    public static List<String> getRolesFromAuthAnnotation(String roleNames) {
        List<String> roles = new ArrayList<>();
        if (roleNames == null) {
            return roles;
        }
        for (String role : roleNames.split(",")) {
            if (!role.trim().isEmpty()) {
                roles.add(role.trim());
            }
        }
        return roles;
    }

    public static boolean verifyRoleAccess(Object roleAuth, String roleNames) {
        List<String> roles = getRolesFromAuthAnnotation(roleNames);
        if (roles.isEmpty()) {
            return true;
        }
        return roleAuth != null && roles.contains(roleAuth.toString());
    }
}
