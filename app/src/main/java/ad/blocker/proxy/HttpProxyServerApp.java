package ad.blocker.proxy;

import ad.blocker.proxy.intercept.common.FullResponseIntercept;
import ad.blocker.proxy.server.HttpProxyServer.*;
import ad.blocker.proxy.server.*;
import ad.blocker.proxy.intercept.*;
import ad.blocker.proxy.util.HttpUtil;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.nio.charset.Charset;


/**
 * @Author LiWei
 * @Description
 * @Date 2019/9/23 17:30
 */

public class HttpProxyServerApp {
    public static void main(String[] args) {
        System.out.println("start proxy server");
        int port = 9999;
        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
//        new HttpProxyServer().start(port);
        HttpProxyServerConfig config =  new HttpProxyServerConfig();
//å¼å¯HTTPSæ¯æ
//ä¸å¼å¯çè¯HTTPSä¸ä¼è¢«æ¦æªï¼èæ¯ç´æ¥è½¬ååå§æ¥æ
        config.setHandleSsl(true);
        new HttpProxyServer()
                .serverConfig(config)
                .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline) {
                        pipeline.addLast(new FullResponseIntercept() {

                            @Override
                            public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                                //å¨å¹éå°ç¾åº¦é¦é¡µæ¶æå¥js
                                return true;
//                                return HttpUtil.checkUrl(pipeline.getHttpRequest(), "^test.rucstone.xyz$")
//                                        && isHtml(httpRequest, httpResponse);
                            }

                            @Override
                            public void handleResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                                //æå°åå§ååºä¿¡æ¯
                                System.out.println(httpResponse.toString());
                                System.out.println(httpResponse.content().toString(Charset.defaultCharset()));
                                //ä¿®æ¹ååºå¤´åååºä½
                                httpResponse.headers().set("handel", "edit head");
                                System.out.println(httpResponse.content().toString());
                                httpResponse.content().writeBytes(("<script>alert('hello proxyee');</script>").getBytes());
                            }
                        });
                    }
                })
                .start(9999);

    }
}
