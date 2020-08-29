package org.codedefenders.servlets.auth;

import org.apache.shiro.web.env.DefaultWebEnvironment;

public class CodeDefendersWebEnvironment extends DefaultWebEnvironment {

    public CodeDefendersWebEnvironment() {
        super();
        setFilterChainResolver(CodeDefendersHelper.getFilterChainResolver());
        setSecurityManager(CodeDefendersHelper.getSecurityManager());
    }

}