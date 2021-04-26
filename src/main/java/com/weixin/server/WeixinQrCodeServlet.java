package com.weixin.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;

@WebServlet("/weixinqrcode")
public class WeixinQrCodeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static String accessToken = null;
	private static String qrCodeTicket = null;
	private static final Logger logger = LogManager.getLogger(WeixinQrCodeServlet.class);
	private static final String charSet = "UTF-8";
	private static final String QRCODETYPE = "QRCODE_LOGIN";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		accessToken = WeixinUtils.getAccessToken();
		String cacheKey = request.getParameter("key");
		logger.info("当前用户缓存key:{}", cacheKey);
		WeixinCache.put(cacheKey, "none");
		WeixinCache.put(cacheKey + "_done", false);
		if (qrCodeTicket == null) {
			qrCodeTicket = WeixinUtils.getQrCodeTiket(accessToken, QRCODETYPE, cacheKey);
		}
		InputStream in = WeixinUtils.getQrCodeStream(qrCodeTicket);
		response.setContentType("image/jpeg; charset=utf-8");
		OutputStream os = null;
		os = response.getOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		while ((len = in.read(buffer)) != -1) {
			os.write(buffer, 0, len);
		}
		os.flush();
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String cacheKey = request.getParameter("key");
		logger.info("登录轮询读取缓存key:{}", cacheKey);
		Boolean cacheDone = (Boolean) WeixinCache.get(cacheKey + "_done");
		response.setContentType("application/json;charset=utf-8");
		String rquestBody = WeixinUtils.InPutstream2String(request.getInputStream(), charSet);
		logger.info("获取到请求正文");
		logger.info(rquestBody);
		logger.info("是否扫码成功:{}", cacheDone);
		JSONObject ret = new JSONObject();
		if (cacheDone != null && cacheDone) {
			JSONObject bindUser = (JSONObject) WeixinCache.get(cacheKey);
			ret.put("binduser", bindUser);
			ret.put("errcode", 0);
			ret.put("errmsg", "ok");
			WeixinCache.remove(cacheKey);
			WeixinCache.remove(cacheKey + "_done");
			logger.info("已移除缓存数据，key：{}", cacheKey);
			response.getWriter().write(ret.toJSONString());
			return;
		}
		ret.put("errcode", 99);
		ret.put("errmsg", "用户还未扫码");
		response.getWriter().write(ret.toJSONString());
	}

}
