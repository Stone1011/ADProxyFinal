package ad.blocker;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import ad.blocker.connect.feature.manager.view.ProxyManagerFragment;
import ad.blocker.connect.feature.manager.viewmodel.ProxyManagerViewModel;
import ad.blocker.filter.Filter;
import ad.blocker.proxy.crt.CertUtilsLoader;
import ad.blocker.proxy.intercept.HttpProxyInterceptInitializer;
import ad.blocker.proxy.intercept.HttpProxyInterceptPipeline;
import ad.blocker.proxy.intercept.common.FullResponseIntercept;
import ad.blocker.proxy.server.HttpProxyServer;
import ad.blocker.proxy.server.HttpProxyServerConfig;
import dagger.hilt.android.AndroidEntryPoint;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity
{
    public static Filter filter = new Filter();
    public static Context context;
    public static boolean first = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        CertUtilsLoader.init();
        context = this;

        int perm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS);
        if (perm == PackageManager.PERMISSION_DENIED)
        {
            showPermissionRequiredDialog();
        }

        if (Build.VERSION.SDK_INT>=23&& ContextCompat.checkSelfPermission(MainActivity.this, WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (Build.VERSION.SDK_INT>=23&& ContextCompat.checkSelfPermission(MainActivity.this, READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_EXTERNAL_STORAGE}, 1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()){
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        }

        perm = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
        if (perm == PackageManager.PERMISSION_DENIED)
        {
            showStoragePermissionRequiredDialog();
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

        showApplicationContent(savedInstanceState);
    }

    private void showPermissionRequiredDialog()
    {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(getString(R.string.dialog_title_special_permissions)).
                setMessage(getString(R.string.dialog_message_special_permissions)).
                setCancelable(false).create();
        dialog.show();
    }

    private void showStoragePermissionRequiredDialog()
    {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Storage Permission Required").
                setMessage("Storage Permission Required").
                setCancelable(true).create();
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
                        public Pair <Filter.QueryStatus, String> query;

                        @Override
                        public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline)
                        {
                            // isOk
                            if(httpResponse.status().code() != 200)
                                return false;
                            // isHtml
                            if(!judgeHtml(httpResponse))
                                return false;
                            // queryNeed
                            query = filter.query(getURL(httpRequest, pipeline).toLowerCase());
                            if(query.first == Filter.QueryStatus.NO_BLOCK)
                                return false;

                            return true;
                        }

                        @Override
                        public void handleResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline)
                        {
//                            System.out.println(httpResponse.toString());
//                            System.out.println(httpResponse.content().toString(Charset.defaultCharset()));
                            httpResponse.headers().set("AD-Proxy", "Edited");
                            query = filter.query(getURL(httpRequest, pipeline).toLowerCase());
                            if(query.first == Filter.QueryStatus.BLACK)
                            {
                                // TODO: need to figure out how to use netty
                                httpResponse.content().clear();
                                httpResponse.setStatus(HttpResponseStatus.valueOf(403));
                                Log.i("handleResponse", String.format("Black list URL: %s", httpRequest.uri().toLowerCase()));
                                return ;
                            }
//                            System.out.println(httpResponse.content().toString());
//                            httpResponse.content().writeBytes(("<script>alert('hello proxyee');</script>").getBytes());
                            Log.i("handleResponse", String.format("Modify URL: %s", httpRequest.uri().toLowerCase()));
                            Log.d("handleResponse", String.format("Contents to add: %s", query.second));
                            int size = httpResponse.content().readableBytes();
                            byte [] content = new byte[size];
                            httpResponse.content().readBytes(content);
                            String str = new String(content);
                            byte [] modified;
                            modified = modifyResponse(content, query.second);
                            String modifiedStr = new String(modified);
//                            Log.d("handleResponse", modifiedStr);

                            httpResponse.content().clear();
                            httpResponse.content().writeBytes(modified);

                            Log.i("handleResponse", String.format("Readable length: %d", httpResponse.content().readableBytes()));
                        }
                    });
                }
            }).start(port);
        }
    }

    private static boolean judgeHtml(HttpResponse response)
    {
        String contentType = response.headers().get(HttpHeaderNames.CONTENT_TYPE).toLowerCase();
        return contentType.contains("html");
    }

    private static byte [] modifyResponse(byte [] content, String add)
    {
        byte [] modified = new byte [content.length + add.length()];
        byte [] mark = "</head>".getBytes();
        byte [] toAdd = add.getBytes();
        int pos = -1;  // '<' position of </head>
        for(int i = 0; i < content.length - mark.length; i++)
        {
            boolean flag = true;
            for(int j = 0; j < mark.length; j++)
            {
                if(content[i+j] != mark[j])
                {
                    flag = false;
                    break;
                }
            }
            if(flag)
                pos = i;
        }

        if(pos != -1)
        {
            for(int i = 0; i < pos; i++)
                modified[i] = content[i];
            for(int i = pos; i < pos + toAdd.length; i++)
                modified[i] = toAdd[i - pos];
            for(int i = pos; i < content.length; i++)
                modified[i + toAdd.length] = content[i];

            return modified;
        }
        else
            return content;
    }

    private static String getURL(HttpRequest request, HttpProxyInterceptPipeline pipeline)
    {
        String protocol;
        if (pipeline.getRequestProto().getPort() == 443)
            protocol = "https://";
        else
            protocol = "http://";

        String host = request.headers().get(HttpHeaderNames.HOST);
        String path = request.uri();

        if(host.charAt(host.length() - 1) == '/')
            return protocol + host.substring(0, host.length() - 1) + path;
        else
            return protocol + host + path;
    }
}

