package org.codedefenders.beans.user;

import java.io.Serializable;

import javax.annotation.ManagedBean;
import javax.annotation.Priority;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Alternative;

import org.apache.shiro.SecurityUtils;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.auth.CodeDefendersRealm;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.User;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.util.CDIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Keeps track of the logged in user and other login information (URL to redirect to after login).</p>
 * <p>Bean Name: {@code login}</p>
 *
 * @deprecated Use {@link CodeDefendersAuth} instead, if via simple Injection possible!
 */
@Deprecated
@ManagedBean
@SessionScoped
@Alternative
@Priority(0)
public class LoginBean implements Serializable, CodeDefendersAuth {
    private static final Logger logger = LoggerFactory.getLogger(LoginBean.class);

    public LoginBean() {
    }

    /**
     * Checks if an active user is logged in for this session.
     *
     * @return If an active user is logged in for this session.
     */
    @Override
    public boolean isLoggedIn() {
        return SecurityUtils.getSubject().isAuthenticated();
        //return user != null && user.isActive();
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    /**
     * Returns the id of the logged-in user for the session.
     *
     * @return The id of the logged-in user for the session.
     */
    @Override
    public int getUserId() {
        return SecurityUtils.getSubject().getPrincipals().oneByType(CodeDefendersRealm.LocalUserId.class).getUserId();
        //return user.getId();
    }

    @Override
    public SimpleUser getSimpleUser() {
        return CDIUtil.getBeanFromCDI(UserService.class).getSimpleUserById(getUserId()).orElse(null);
    }

    @Deprecated
    @Override
    public String getUsername() {
        return getSimpleUser().getName();
    }

    @Override
    public User getUser() {
        return CDIUtil.getBeanFromCDI(UserService.class).getUserById(getUserId()).orElse(null);
    }

    /**
     * Returns the logged-in user for the session.
     *
     * @return The logged-in user for the session.
     */
    @Deprecated
    public UserEntity getUserEntity() {
        return CDIUtil.getBeanFromCDI(UserRepository.class).getUserById(getUserId()).orElse(null);
    }
}
