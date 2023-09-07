/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.itests.http.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

public class WebClientFactory {

    private static int TIMEOUT = 60000;

    private static Collection<WebClient> clients = new ArrayList<>();

    public static WebClient getNewWebClient() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

        // webClient = new WebClient(BrowserVersion.CHROME);
        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);

        webClient.getOptions().setCssEnabled(true);
        webClient.setCssErrorHandler(new SilentCssErrorHandler());

        // Do not fail on status code ?
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        // Disable test failing because of JS exceptions
        webClient.getOptions().setThrowExceptionOnScriptError(false);

        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setAppletEnabled(false);

        webClient.getOptions().setJavaScriptEnabled(true);

        webClient.getOptions().setPopupBlockerEnabled(true);
        webClient.getOptions().setTimeout(TIMEOUT);
        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.setAlertHandler((page, message) -> System.err.println("[alert] " + message));
        // Shut down HtmlUnit
        // webClient.setIncorrectnessListener(new IncorrectnessListener() {
        //
        // @Override
        // public void notify(String arg0, Object arg1) {
        // // TODO Auto-generated method stub
        //
        // }
        // });
        webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

            @Override
            public void scriptException(InteractivePage page, ScriptException scriptException) {
                // TODO Auto-generated method stub

            }

            @Override
            public void timeoutError(InteractivePage page, long allowedTime, long executionTime) {
                // TODO Auto-generated method stub

            }

            @Override
            public void malformedScriptURL(InteractivePage page, String url,
                    MalformedURLException malformedURLException) {
                // TODO Auto-generated method stub

            }

            @Override
            public void loadScriptError(InteractivePage page, URL scriptUrl, Exception exception) {
                // TODO Auto-generated method stub

            }
        });
        webClient.waitForBackgroundJavaScript(TIMEOUT);

        webClient.setRefreshHandler(new WaitingRefreshHandler());
        clients.add(webClient);

        return webClient;
    }

    public static void closeAllClients() {
        for (WebClient webClient : clients) {
            if (webClient != null) {
                webClient.close();
            }
        }
    }
}
