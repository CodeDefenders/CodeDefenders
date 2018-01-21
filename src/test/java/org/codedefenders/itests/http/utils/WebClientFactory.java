package org.codedefenders.itests.http.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.AlertHandler;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

public class WebClientFactory {
	
	private static int TIMEOUT = 60000;
	
	private static Collection<WebClient> clients = new ArrayList<WebClient>();

		public static WebClient getNewWebClient() {
			java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
			java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

			// webClient = new WebClient(BrowserVersion.CHROME);
			WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
			//
			webClient.getOptions().setCssEnabled(true);
			webClient.setCssErrorHandler(new SilentCssErrorHandler());
			//
			// Do not fail on status code ?
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			// Disable test failing because of JS exceptions
			webClient.getOptions().setThrowExceptionOnScriptError(false);
			//
			webClient.getOptions().setRedirectEnabled(true);
			webClient.getOptions().setAppletEnabled(false);
			//
			webClient.getOptions().setJavaScriptEnabled(true);
			//
			webClient.getOptions().setPopupBlockerEnabled(true);
			webClient.getOptions().setTimeout(TIMEOUT);
			webClient.getOptions().setPrintContentOnFailingStatusCode(false);
			webClient.setAjaxController(new NicelyResynchronizingAjaxController());
			webClient.setAlertHandler(new AlertHandler() {

				public void handleAlert(Page page, String message) {
					System.err.println("[alert] " + message);
				}

			});
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
