package copy.links.copylinks;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity {
    private WebServer server;
    private Context context;
    private String htmlResponse;
    private String phoneText;
    private TextView urlView;
    private EditText phoneEditText;
    private ClipboardManager clipboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        server = new WebServer();
        htmlResponse = "";
        phoneText="";
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        urlView = (TextView) this.findViewById(R.id.urlview);
        phoneEditText = (EditText) this.findViewById(R.id.phoneText);
        setUrlView();

        try {
            server.start();
        } catch (IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
        Log.w("Httpd", "Web server initialized.");

    }

    private void setUrlView() {
        WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        ip = "http://" + ip + ":8086";
        urlView.setText(ip);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null)
            server.stop();
    }

    private class WebServer extends NanoHTTPD {

        public WebServer() {
            super(8086);
        }

        @Override
        public Response serve(IHTTPSession session) {

            //copy the html form response from index.html
            if (htmlResponse.equals("")) {
                try {
                    AssetManager assetManager = getApplicationContext().getAssets();
                    InputStream stream = assetManager.open("index.html");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        htmlResponse += line;
                    }
                    reader.close();

                } catch (IOException ioe) {
                    Log.w("Httpd", ioe.toString());
                } catch (Exception e) {
                    Log.w("Httpd", e.getMessage());
                }
            }

            if (session.getMethod().name().equals("POST")) {
                try {
                    //extracting the post parameters
                    session.parseBody(new HashMap<String, String>());
                    final String inputText = session.getParameters().get("UserInput").get(0);
                    Log.w("Httpd", session.getMethod() + " " + session.getParms());

                    //UI thread functions. getting data to clipboard
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if(!inputText.equals("")) {
                                Toast.makeText(context, inputText, Toast.LENGTH_SHORT).show();
                                ClipData clip = ClipData.newPlainText("inputText", inputText);
                                clipboard.setPrimaryClip(clip);
                            }
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ResponseException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.w("Httpd", e.getMessage());
                }
            }

            //set phone text to html response
            setPhoneTextToResponse();
            return newFixedLengthResponse(htmlResponse);
        }

        private void setPhoneTextToResponse() {
            htmlResponse=htmlResponse.replaceFirst("value=\""+phoneText+"\"", "value=\"" + phoneEditText.getText().toString() + "\"");
            phoneText = phoneEditText.getText().toString();
        }
    }

}

