package com.weixin.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@WebServlet("/weixinbind")
public class WeixinBindServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String charSet = "UTF-8";
	private static final Logger logger = LogManager.getLogger(WeixinBindServlet.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String key = request.getParameter("key");
		String userName = request.getParameter("u");
		String password = request.getParameter("p");
		JSONObject user = findUserByNameAndPwd(userName, password);
		JSONObject ret = new JSONObject();
		if (user == null) {
			ret.put("errcode", 99);
			ret.put("errmsg", "未找到用户信息");
			response.getWriter().print(ret.toJSONString());
			return;
		}
		//  获取openID
		String openId = (String) WeixinCache.get(key);
		logger.info("获取到缓存openid {}", openId);
		user.put("openId", openId);
		saveBindData(user);
		ret.put("errcode", 0);
		ret.put("errmsg", "绑定成功");
		response.getWriter().print(ret.toJSONString());
		saveBindData(user);
		return;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private JSONObject findUserByNameAndPwd(String name, String pwd) throws IOException {
		InputStream is = this.getServletContext().getResourceAsStream("/WEB-INF/userdata.json");
		String serverUserJson = WeixinUtils.InPutstream2String(is, charSet);
		logger.info("读取到服务端存储JSON文件 {}", serverUserJson);
		JSONArray users = JSONObject.parseArray(serverUserJson);
		for (int i = 0, len = users.size(); i < len; i++) {
			JSONObject user = users.getJSONObject(i);
			if (name.equals(user.getString("userName")) && pwd.equals(user.getString("password"))) {
				return user;
			}
		}
		return null;
	}

	private void saveBindData(JSONObject user) throws IOException {
		String path = this.getServletContext().getRealPath("/WEB-INF/userdata.json");
		logger.info("数据文件保存路径:{}", path);
		PrintStream ps = new PrintStream(path);
		ps.print("[" + user.toJSONString() + "]");
		ps.flush();
		ps.close();
	}
}
