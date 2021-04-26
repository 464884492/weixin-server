package com.weixin.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@WebServlet("/weixinmsgevent")
public class WeixinMsgEventServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger(WeixinMsgEventServlet.class);
	private static final String TOKEN = "weixinmsgevent";
	private static final String charSet = "UTF-8";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		logger.info("微信在配置服务器传递验证参数");
		Map<String, String[]> reqParam = request.getParameterMap();
		for (String key : reqParam.keySet()) {
			logger.info(" {} = {}", key, reqParam.get(key));
		}

		String signature = request.getParameter("signature");
		String echostr = request.getParameter("echostr");
		String timestamp = request.getParameter("timestamp");
		String nonce = request.getParameter("nonce");

		String buildSign = WeixinUtils.getSignature(TOKEN, timestamp, nonce);

		logger.info("服务器生成签名信息:{}", buildSign);
		if (buildSign.equals(signature)) {
			response.getWriter().write(echostr);
			logger.info("服务生成签名与微信服务器生成签名相等，验证成功");
			return;
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String rquestBody = WeixinUtils.InPutstream2String(request.getInputStream(), charSet);
		logger.info("获取到微信推送消息正文");
		logger.info(rquestBody);
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			dbf.setXIncludeAware(false);
			dbf.setExpandEntityReferences(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			StringReader sr = new StringReader(rquestBody);
			InputSource is = new InputSource(sr);
			Document document = db.parse(is);
			Element root = document.getDocumentElement();
			NodeList fromUserName = document.getElementsByTagName("FromUserName");
			String openId = fromUserName.item(0).getTextContent();
			logger.info("获取到扫码用户openid:{}", openId);
			NodeList msgType = root.getElementsByTagName("MsgType");
			String msgTypeStr = msgType.item(0).getTextContent();
			if ("event".equals(msgTypeStr)) {
				NodeList event = root.getElementsByTagName("Event");
				String eventStr = event.item(0).getTextContent();
				logger.info("获取到event类型:{}", eventStr);
				if ("SCAN".equals(eventStr)) {
					NodeList eventKey = root.getElementsByTagName("EventKey");
					String eventKeyStr = eventKey.item(0).getTextContent();
					logger.info("获取到扫码场景值:{}", eventKeyStr);

					if (eventKeyStr.indexOf("QRCODE_LOGIN") == 0) {
						String cacheKey = eventKeyStr.split("#")[1];
						scanLogin(openId, cacheKey);
					}
				}
			}
			if ("text".equals(msgTypeStr)) {
				NodeList content = root.getElementsByTagName("Content");
				String contentStr = content.item(0).getTextContent();
				logger.info("用户发送信息:{}", contentStr);
			}
		} catch (Exception e) {
			logger.error("微信调用服务后台出现错误", e.getCause());
		}
	}

	private void scanLogin(String openId, String cacheKey) throws IOException {
		JSONObject user = findUserByOpenId(openId);
		if (user == null) {
			// 发送消息让用户绑定账号
			logger.info("用户还未绑定微信，正在发送邀请绑定微信消息");
			WeixinUtils.sendTempalteMsg(WeixinUtils.getAccessToken(), openId,
					"LWP44mgp0rEGlb0pK6foatU0Q1tWhi5ELiAjsnwEZF4",
					"http://pro.vaiwan.com/weixin-server/weixinbind.html?key=" + cacheKey, null);
			WeixinCache.put(cacheKey, openId);
			return;
		}
		// 更新缓存
		WeixinCache.put(cacheKey, user);
		WeixinCache.put(cacheKey + "_done", true);
		logger.info("已将缓存标志[key]:{}设置为true", cacheKey + "_done");
		logger.info("已更新缓存[key]:{}", cacheKey);
		logger.info("已发送登录成功微信消息");
		WeixinUtils.sendTempalteMsg(WeixinUtils.getAccessToken(), openId, "MpiOChWEygaviWsIB9dUJLFGXqsPvAAT2U5LcIZEf_o",
				null, null);
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
}
