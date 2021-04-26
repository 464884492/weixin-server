package com.weixin.server;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@WebServlet("/weixinredirect")
public class WeixinRedirectServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger(WeixinRedirectServlet.class);
	private static final String charSet = "UTF-8";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String code = request.getParameter("code");
		String state = request.getParameter("state");
		logger.info("获取到微信回传参数code:{},state:{}", code, state);
		JSONObject webTokenInfo = WeixinUtils.getWebAuthTokenInfo(code);
		if (webTokenInfo != null && !webTokenInfo.containsKey("errcode")) {
			String openId = webTokenInfo.getString("openid");
			logger.info("获取到用opeind", openId);
			JSONObject user = findUserByOpenId(openId);
			if (user == null) {
				//用户未绑定 将openid存入缓存方便下一步绑定用户
				WeixinCache.put(state, openId);
				response.sendRedirect("weixinbind.html?key=" + state);
				return;
			}
			WeixinCache.put(state, user);
			WeixinCache.put(state + "_done", true);
			logger.info("已将缓存标志[key]:{}设置为true", state + "_done");
			logger.info("已更新缓存[key]:{}", state);

			response.setCharacterEncoding("GBK");
			response.getWriter().print("扫码成功，已成功登录系统");
		}
	}

	private JSONObject findUserByOpenId(String openId) throws IOException {
		InputStream is = this.getServletContext().getResourceAsStream("/WEB-INF/userdata.json");
		String serverUserJson = WeixinUtils.InPutstream2String(is, charSet);
		logger.info("读取到服务端存储JSON文件 {}", serverUserJson);
		JSONArray users = JSONObject.parseArray(serverUserJson);
		for (int i = 0, len = users.size(); i < len; i++) {
			JSONObject user = users.getJSONObject(i);
			if (openId.equals(user.getString("openId"))) {
				return user;
			}
		}
		return null;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
