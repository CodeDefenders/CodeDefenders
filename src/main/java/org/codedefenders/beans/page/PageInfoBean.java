package org.codedefenders.beans.page;

import jakarta.enterprise.context.RequestScoped;

/**
 * <p>Provides various page info, like the page title (and maybe other things later).</p>
 * <p>Bean Name: {@code pageInfo}</p>
 */
@RequestScoped
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
