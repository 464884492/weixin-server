package com.weixin.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WeixinUtils {
	private static final Logger logger = LogManager.getLogger(WeixinUtils.class);
	private static final MediaType JSON = MediaType.parse("application/json;charset=utf-8");
	private static String ACCESSTOKEN = null;
	private static final String APPID = "wx859fbe49678386af";
	private static final String APPSECRET = "替换真实APPSECRET";
	private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={0}&secret={1}";
	private static final String QRCODE_TICKET_URL = "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token={0}";
	private static final String QRCODE_SRC_URL = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket={0}";
	private static final String STENDTEMPLATE_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token={0}";
	private static final String WEB_AUTH_URL = "https://open.weixin.qq.com/connect/oauth2/authorize?appid={0}&redirect_uri={1}&response_type=code&scope=snsapi_base&state={2}#wechat_redirect";
	private static final String WEB_AUTH_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={0}&secret={1}&code={2}&grant_type=authorization_code";
	public static final String charSet = "UTF-8";

	public static String getSignature(String token, String timestamp, String nonce) {

		String[] array = new String[] { token, timestamp, nonce };
		Arrays.sort(array);
		StringBuffer sb = new StringBuffer();
		for (String str : array) {
			sb.append(str);
		}
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(sb.toString().getBytes());
			byte[] digest = md.digest();
			StringBuffer hexStr = new StringBuffer();
			String shaHex = "";
			for (int i = 0; i < digest.length; i++) {
				shaHex = Integer.toHexString(digest[i] & 0xFF);
				if (shaHex.length() < 2) {
					hexStr.append(0);
				}
				hexStr.append(shaHex);
			}
			return hexStr.toString();

		} catch (NoSuchAlgorithmException e) {
			logger.error("获取签名信息失败", e.getCause());
		}
		return "";
	}

	public static String getAccessToken() {
		if (ACCESSTOKEN != null) {
			logger.info("从内存中获取到AccessToken:{}", ACCESSTOKEN);
			return ACCESSTOKEN;
		}
		String access_token_url = MessageFormat.format(ACCESS_TOKEN_URL, APPID, APPSECRET);
		logger.info("access_token_url转换后的访问地址:{}", access_token_url);
		Request request = new Request.Builder().url(access_token_url).build();
		OkHttpClient httpClient = new OkHttpClient();
		Call call = httpClient.newCall(request);
		try {
			Response response = call.execute();
			String resBody = response.body().string();
			logger.info("获取到相应正文:{}", resBody);
			JSONObject jo = JSONObject.parseObject(resBody);
			String accessToken = jo.getString("access_token");
			String errCode = jo.getString("errcode");
			if (StringUtils.isBlank(errCode)) {
				errCode = "0";
			}
			if ("0".equals(errCode)) {
				logger.info("获取accessToken成功,值为：{}", accessToken);
				ACCESSTOKEN = accessToken;
			}

			return accessToken;
		} catch (IOException e) {
			logger.error("获取accessToken出现错误", e.getCause());
		}
		return null;
	}

	public static String getQrCodeTiket(String accessToken, String qeCodeType, String qrCodeValue) {
		String qrcode_ticket_url = MessageFormat.format(QRCODE_TICKET_URL, accessToken);
		logger.info("qrcode_ticket_url转换后的访问地址:{}", qrcode_ticket_url);

		JSONObject pd = new JSONObject();
		pd.put("expire_seconds", 604800);
		pd.put("action_name", "QR_STR_SCENE");
		JSONObject sence = new JSONObject();
		sence.put("scene", JSONObject
				.parseObject("{\"scene_str\":\"" + MessageFormat.format("{0}#{1}", qeCodeType, qrCodeValue) + "\"}"));
		pd.put("action_info", sence);
		logger.info("提交内容{}", pd.toJSONString());
		RequestBody body = RequestBody.create(JSON, pd.toJSONString());

		Request request = new Request.Builder().url(qrcode_ticket_url).post(body).build();
		OkHttpClient httpClient = new OkHttpClient();
		Call call = httpClient.newCall(request);
		try {
			Response response = call.execute();
			String resBody = response.body().string();
			logger.info("获取到相应正文:{}", resBody);
			JSONObject jo = JSONObject.parseObject(resBody);
			String qrTicket = jo.getString("ticket");
			String errCode = jo.getString("errcode");
			if (StringUtils.isBlank(errCode)) {
				errCode = "0";
			}
			if ("0".equals(jo.getString(errCode))) {
				logger.info("获取QrCodeTicket成功,值为：{}", qrTicket);
			}
			return qrTicket;
		} catch (IOException e) {
			logger.error("获取QrCodeTicket出现错误", e.getCause());
		}
		return null;
	}

	public static InputStream getQrCodeStream(String qrCodeTicket) {
		String qrcode_src_url = MessageFormat.format(QRCODE_SRC_URL, qrCodeTicket);
		logger.info("qrcode_src_url转换后的访问地址:{}", qrcode_src_url);
		Request request = new Request.Builder().url(qrcode_src_url).get().build();
		OkHttpClient httpClient = new OkHttpClient();
		Call call = httpClient.newCall(request);
		try {
			Response response = call.execute();
			return response.body().byteStream();
		} catch (IOException e) {
			logger.error("获取qrcode_src_url出现错误", e.getCause());
		}
		return null;
	}

	public static void sendTempalteMsg(String accessToken, String openId, String templateId, String url, Map data) {
		String sendTemplate_url = MessageFormat.format(STENDTEMPLATE_URL, accessToken);
		logger.info("sendTemplate_url 转换后的访问地址:{}", sendTemplate_url);
		JSONObject jo = new JSONObject();
		jo.put("touser", openId);
		jo.put("template_id", templateId);
		jo.put("url", url);
		jo.put("data", data);
		logger.info("提交内容{}", jo.toJSONString());
		RequestBody body = RequestBody.create(JSON, jo.toJSONString());
		Request request = new Request.Builder().url(sendTemplate_url).post(body).build();
		OkHttpClient client = new OkHttpClient();
		try (Response response = client.newCall(request).execute()) {
			String resBody = response.body().string();
			logger.info("获取到相应正文:{}", resBody);

		} catch (IOException e) {
			logger.error("发送模板消息出现错误", e.getCause());
		}
	}

	public static String InPutstream2String(InputStream aStream, String charSet) throws IOException {
		try {
			byte[] fBuffer = new byte[8192];
			ByteArrayOutputStream ftemp = new ByteArrayOutputStream(8192);
			int flen;
			do {
				flen = aStream.read(fBuffer, 0, fBuffer.length);
				if (flen > 0)
					ftemp.write(fBuffer, 0, flen);
			} while (flen >= 0);
			return new String(ftemp.toByteArray(), charSet);
		} finally {
			aStream.close();
		}
	}

	public static BufferedImage buildWebAuthUrlQrCode(String redirectUrl, String state) {
		logger.info("获取到参数信息redirectUrl :{},state:{}", redirectUrl, state);
		try {
			redirectUrl = URLEncoder.encode(redirectUrl, charSet);
			state = URLEncoder.encode(state, charSet);

			String web_auth_url = MessageFormat.format(WEB_AUTH_URL, APPID, redirectUrl, state);
			logger.info("web_auth_url 转换后的访问地址:{}", web_auth_url);
			QRCodeWriter qrCodeWriter = new QRCodeWriter();
			BitMatrix bitMatrix = qrCodeWriter.encode(web_auth_url, BarcodeFormat.QR_CODE, 500, 500);
			return MatrixToImageWriter.toBufferedImage(bitMatrix);
		} catch (Exception e) {
			logger.error("生成web授权二维码出现异常", e.getCause());
			return null;
		}
	}

	public static JSONObject getWebAuthTokenInfo(String code) {
		String web_auth_token_url = MessageFormat.format(WEB_AUTH_TOKEN_URL, APPID, APPSECRET, code);
		logger.info("web_auth_token_url 转换后的访问地址:{}", web_auth_token_url);
		Request request = new Request.Builder().url(web_auth_token_url).build();
		OkHttpClient client = new OkHttpClient();
		try (Response response = client.newCall(request).execute()) {
			String resBody = response.body().string();
			logger.info("获取到相应正文:{}", resBody);
			return JSONObject.parseObject(resBody);
		} catch (IOException e) {
			logger.error("获取webauthtoken出现异常", e.getCause());
		}
		return null;
	}
}
