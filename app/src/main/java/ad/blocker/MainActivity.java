package ad.blocker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import ad.blocker.connect.feature.manager.view.ProxyManagerFragment;
import ad.blocker.proxy.crt.CertUtilsLoader;
import ad.blocker.proxy.intercept.HttpProxyInterceptInitializer;
import ad.blocker.proxy.intercept.HttpProxyInterceptPipeline;
import ad.blocker.proxy.intercept.common.FullResponseIntercept;
import ad.blocker.proxy.server.HttpProxyServer;
import ad.blocker.proxy.server.HttpProxyServerConfig;
import dagger.hilt.android.AndroidEntryPoint;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        CertUtilsLoader.init();

        int perm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS);
        if (perm == PackageManager.PERMISSION_DENIED)
        {
            showPermissionRequiredDialog();
        }
        else if (perm == PackageManager.PERMISSION_GRANTED)
        {
            showApplicationContent(savedInstanceState);
        }

        try
        {
            SSLContext.getInstance("TLSv1.2");
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        try
        {
            ProviderInstaller.installIfNeeded(getApplicationContext());
        }
        catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e)
        {
            e.printStackTrace();
        }
    }

    private void showPermissionRequiredDialog()
    {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(getString(R.string.dialog_title_special_permissions)).setMessage(getString(R.string.dialog_message_special_permissions)).setCancelable(false).create();
        dialog.show();
    }

    private void showApplicationContent(Bundle savedInstanceState)
    {
        if (savedInstanceState == null)
        {
            getSupportFragmentManager().beginTransaction().
                    replace(R.id.container, new ProxyManagerFragment()).commitNow();
        }
    }

    public static void startProxy(Context context, int port)
    {
        StartProxy sp = new StartProxy(context, port);
        new Thread(sp).start();
    }

    private static class StartProxy implements Runnable
    {
        Context context;
        int port;

        StartProxy(Context con, int pot)
        {
            context = con;
            port = pot;
        }

        @Override
        public void run()
        {
            HttpProxyServerConfig config = new HttpProxyServerConfig();
            config.setHandleSsl(true);
            new HttpProxyServer().serverConfig(config).serverContext(context).proxyInterceptInitializer(new HttpProxyInterceptInitializer()
            {
                @Override
                public void init(HttpProxyInterceptPipeline pipeline)
                {
                    pipeline.addLast(new FullResponseIntercept()
                    {
                        @Override
                        public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline)
                        {
                            return true;
//                        return HttpUtil.checkUrl(pipeline.getHttpRequest(), "^test.rucstone.xyz$")
//                                && isHtml(httpRequest, httpResponse);
                        }

                        @Override
                        public void handleResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline)
                        {
                            System.out.println(httpResponse.toString());
                            System.out.println(httpResponse.content().toString(Charset.defaultCharset()));

                            httpResponse.headers().set("handel", "edit head");
                            System.out.println(httpResponse.content().toString());
                            httpResponse.content().writeBytes(("<script>alert('hello proxyee');</script>").getBytes());
                        }
                    });
                }
            }).start(port);
        }
    }
}