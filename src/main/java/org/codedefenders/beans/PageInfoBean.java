package org.codedefenders.beans;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

@ManagedBean
@RequestScoped
public class PageInfoBean {
    private  String pageTitle;
    // Put additional information for headers and such here ...

    public String getPageTitle() {
        return pageTitle == null ? "Code Defenders" : pageTitle;
    }

    public boolean hasPageTitle() {
        return pageTitle != null;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }
}
