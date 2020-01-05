package org.codedefenders.beans.page;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@ManagedBean
@RequestScoped
@Named("pageInfo")
public class PageInfoBean {
    private String pageTitle;
    // Put additional information for headers and such here ...

    public PageInfoBean() {
        pageTitle = null;
    }

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
