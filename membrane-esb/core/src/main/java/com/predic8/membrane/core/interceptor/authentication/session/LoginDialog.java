/* Copyright 2012 predic8 GmbH, www.predic8.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */
package com.predic8.membrane.core.interceptor.authentication.session;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.ErrorHandler;
import com.floreysoft.jmte.message.ParseException;
import com.floreysoft.jmte.token.Token;
import com.predic8.membrane.core.Constants;
import com.predic8.membrane.core.Router;
import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.interceptor.Outcome;
import com.predic8.membrane.core.interceptor.authentication.session.SessionManager.Session;
import com.predic8.membrane.core.interceptor.server.WebServerInterceptor;
import com.predic8.membrane.core.util.URLParamUtil;

public class LoginDialog {
	private static Log log = LogFactory.getLog(LoginDialog.class.getName());

	private String loginPath;

	private final UserDataProvider userDataProvider;
	private final TokenProvider tokenProvider;
	private final SessionManager sessionManager;
	
	private final WebServerInterceptor wsi;
	
	public LoginDialog(UserDataProvider userDataProvider, TokenProvider tokenProvider, SessionManager sessionManager, String loginDir, String loginPath) {
		this.loginPath = loginPath;
		this.userDataProvider = userDataProvider;
		this.tokenProvider = tokenProvider;
		this.sessionManager = sessionManager;
		
		wsi = new WebServerInterceptor();
		wsi.setDocBase(loginDir);
	}

	public void init(Router router) throws Exception {
		wsi.init(router);
	}
	
	public boolean isLoginRequest(Exchange exc) {
		URI uri = URI.create(exc.getRequest().getUri());
		return uri.getPath().startsWith(loginPath);
	}

	private void showPage(Exchange exc, int page, Object... params) throws Exception {
		String target = StringUtils.defaultString(URLParamUtil.getParams(exc).get("target"));
		
		exc.getDestinations().set(0, "/index.html");
		wsi.handleRequest(exc);
		
		Engine engine = new Engine();
		engine.setErrorHandler(new ErrorHandler() {
			
			@Override
			public void error(String arg0, Token arg1, Map<String, Object> arg2) throws ParseException {
				System.out.println(arg0);
				
			}
			
			@Override
			public void error(String arg0, Token arg1) throws ParseException {
				System.out.println(arg0);
				
			}
		});
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("action", StringEscapeUtils.escapeXml(loginPath));
		model.put("target", StringEscapeUtils.escapeXml(target));
		if (page == 1)
			model.put("token", true);
		for (int i = 0; i < params.length; i+=2)
			model.put((String)params[i], params[i+1]);
		
		exc.getResponse().setBodyContent(engine.transform(exc.getResponse().getBody().toString(), model).getBytes(Constants.UTF_8_CHARSET));
	}

	public void handleLoginRequest(Exchange exc) throws Exception {
		Session s = sessionManager.getSession(exc.getRequest());
		
		String uri = exc.getRequest().getUri().substring(loginPath.length()-1);
		if (uri.indexOf('?') >= 0)
			uri = uri.substring(0, uri.indexOf('?'));
		exc.getDestinations().set(0, uri);
		
		if (uri.equals("/logout")) {
			if (s != null)
				s.clear();
			exc.setResponse(Response.redirect(loginPath, false).build());
		} else if (uri.equals("/")) { 
			if (s == null || !s.isPreAuthorized()) {
				if (exc.getRequest().getMethod().equals("POST")) {
					Map<String, String> userAttributes;
					try {
						userAttributes = userDataProvider.verify(URLParamUtil.getParams(exc));
					} catch (NoSuchElementException e) {
						showPage(exc, 0, "error", "INVALID_PASSWORD");
						return;
					} catch (Exception e) {
						log.error(e);
						showPage(exc, 0, "error", "INTERNAL_SERVER_ERROR");
						return;
					}
					showPage(exc, 1);
					sessionManager.createSession(exc).preAuthorize(userAttributes);
					tokenProvider.requestToken(userAttributes);
				} else {
					showPage(exc, 0);
				}
			} else {
				if (exc.getRequest().getMethod().equals("POST")) {
					String token = URLParamUtil.getParams(exc).get("token");
					try {
						tokenProvider.verifyToken(s.getUserAttributes(), token);
					} catch (NoSuchElementException e) {
						s.clear();
						showPage(exc, 0, "error", "INVALID_TOKEN");
						return;
					} catch (Exception e) {
						log.error(e);
						s.clear();
						showPage(exc, 0, "error", "INTERNAL_SERVER_ERROR");
						return;
					}
					String target = URLParamUtil.getParams(exc).get("target");
					if (StringUtils.isEmpty(target))
						target = "/";
					exc.setResponse(Response.redirectWithout300(target, false).build());
					s.authorize();
				} else {
					showPage(exc, 1);
				}
			}
		} else {
			wsi.handleRequest(exc);
		}
	}
	
	public Outcome redirectToLogin(Exchange exc) throws MalformedURLException, UnsupportedEncodingException {
		exc.setResponse(Response.
				redirect(loginPath + "?target=" + URLEncoder.encode(exc.getOriginalRequestUri(), "UTF-8"), false).
				header("Pragma", "no-cache").
				header("Cache-Control", "no-cache").
				build());
		return Outcome.RETURN;
	}

}