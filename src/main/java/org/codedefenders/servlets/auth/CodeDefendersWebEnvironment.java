package org.codedefenders.servlets.auth;

import org.apache.shiro.web.env.DefaultWebEnvironment;

// How is this Managed ?!
public class CodeDefendersWebEnvironment extends DefaultWebEnvironment {

    public CodeDefendersWebEnvironment() {
        super();
        setSecurityManager(CodeDefendersHelper.getSecurityManager());
        setFilterChainResolver(CodeDefendersHelper.getFilterChainResolver());
    }
}
