package com.weixin.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@WebServlet("/weixinwebqrcode")
public class WeixinWebQrCodeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger(WeixinWebQrCodeServlet.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String cacheKey = request.getParameter("key");
		logger.info("当前用户缓存key:{}", cacheKey);

		BufferedImage bImg = WeixinUtils.buildWebAuthUrlQrCode("http://pro.vaiwan.com/weixin-server/weixinredirect",
				cacheKey);
		if (bImg != null) {
			response.setContentType("image/png; charset=utf-8");
			OutputStream os = null;
			os = response.getOutputStream();
			ImageIO.write(bImg, "png", os);
			os.flush();
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	}

}
