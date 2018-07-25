package com.iothub.azure.microsoft.com.androidsample;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private final String connString = "HostName=drivesocialhub.azure-devices.net;DeviceId=raspi001;SharedAccessKey=NzKxtH8VfEKNGBtRP4Na0IzgjXRQMrscrcrwIlW0kmA=";
    private final String deviceId = "raspi001";
    private double temperature;
    private double humidity;

    private DeviceClient mClient;
    private static final int METHOD_SUCCESS = 200;

    private static final int METHOD_HUNG = 300;

    private static final int METHOD_NOT_FOUND = 404;

    private static final int METHOD_NOT_DEFINED = 404;

    private TextView mText;

    private static int method_command(Object command)

    {

        System.out.println("invoking command on this device");

        // Insert code to invoke command here

        return METHOD_SUCCESS;

    }
    private static int method_default(Object data)

    {

        System.out.println("invoking default method for this device");

        // Insert device specific code here

        return METHOD_NOT_DEFINED;

    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mText = this.findViewById(R.id.plateView);

        ImageView imageView = this.findViewById(R.id.imageView2);
//        while(true)
//        {
//            double left = 20.0 + Math.random() * 10;
////            double top = 30.0 + Math.random() * 20;
//            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//            params.setMargins((int)left,0,0,0);
//            imageView.setLayoutParams(params);
//            try {
//
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//        }

//        try {
//            SendMessage();
//            Setup();

//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//            params.setMargins(100,200,0,0);
//            mText.setText("something");
//            mText.setLayoutParams(params);
//
//            //2
//            params.setMargins(200,300,0,0);
//            mText.setText("something else");
////            mText.setLayoutParams(params);
//
//            params.setMargins(300,200,0,0);
//            mText.setText("something toally else");
//        } catch (Exception e2)
//        {
//            System.out.println("Exception while opening IoTHub connection: " + e2.toString());
//        }
    }

    private void SendMessage() throws URISyntaxException, IOException
    {
        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        // IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);

        try
        {
            client.open();
        } catch (Exception e2)
        {
            System.err.println("Exception while opening IoTHub connection: " + e2.toString());
        }

        for (int i = 0; i < 5; ++i)
        {
            temperature = 20.0 + Math.random() * 10;
            humidity = 30.0 + Math.random() * 20;

            String msgStr = "{\"deviceId\":\"" + deviceId + "\",\"messageId\":" + i + ",\"temperature\":" + temperature + ",\"humidity\":" + humidity + "}";
            try
            {
                Message msg = new Message(msgStr);
                msg.setProperty("temperatureAlert", temperature > 28 ? "true" : "false");
                msg.setMessageId(java.util.UUID.randomUUID().toString());
                System.out.println(msgStr);
                EventCallback eventCallback = new EventCallback();
                client.sendEventAsync(msg, eventCallback, i);
            } catch (Exception e)
            {
                System.err.println("Exception while sending event: " + e.getMessage());
            }
            try
            {
                Thread.sleep(2000);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        client.closeNow();
    }


    protected class SampleDeviceMethodCallback implements com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodCallback
    {
        private TextView view;

        public SampleDeviceMethodCallback(TextView view)
        {
            this.view = view;
        }

        @Override
        public DeviceMethodData call(String methodName, Object methodData, Object context)
        {
//            TextView view = (TextView) context;
            DeviceMethodData deviceMethodData ;
            int status;
            switch (methodName)
            {
                case "found" :
                    final TextView v = this.view;
                    final Object m = methodData;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            try
                            {
                                JSONObject d = new JSONObject(new String((byte[]) m));
                                String box = d.getString("boundingBox");
                                String plate = d.getString("text");
                                String splits[] = box.split(",");
                                int left = Integer.parseInt(splits[0]) * 1440 / 320;
                                int top = Integer.parseInt(splits[1]) * 1440 / 320 + 1000;

                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                params.setMargins(left, top,0,0);
                                v.setText(plate);
                                v.setLayoutParams(params);
                                v.setVisibility(View.VISIBLE);

                            } catch (JSONException e) {
                                System.out.println("Some error in parsing");
                            }
                        }
                    });
                    status = METHOD_SUCCESS;
                    break;

                case "clear":
                    final TextView v2 = this.view;
                    runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {

                                        v2.setVisibility(View.INVISIBLE);

                                      }
                                  });
                    status = METHOD_SUCCESS;
                    break;
                case "command":
                {
                    status = METHOD_SUCCESS;
                    break;
                }
                default:
                {
                    status = METHOD_NOT_DEFINED;
                    break;
                }
            }
            deviceMethodData = new DeviceMethodData(status, "executed " + methodName);
            return deviceMethodData;

        }

    }

    public void refreshText(View v) throws IOException {

        if (mText.getVisibility() == View.INVISIBLE) {

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            double left = 100.0 + Math.random() * 100;
            double top = 200.0 + Math.random() * 100;
            params.setMargins((int) left, (int) top, 0, 0);
            mText.setText("something" + left);
            mText.setLayoutParams(params);
            mText.setVisibility(View.VISIBLE);
        }
        else
        {
            mText.setVisibility(View.INVISIBLE);
        }
    }

    public void btnMethodOnStop(View v) throws IOException {

        mClient.closeNow();
    }

    private void Setup()  throws URISyntaxException, IOException
    {

        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);


        mClient = client;

        try
        {
            client.open();

            System.out.println("Opened connection to IoT Hub.");

            client.subscribeToDeviceMethod(new SampleDeviceMethodCallback(this.mText), null, new DeviceMethodStatusCallBack(), null);

            System.out.println("Subscribed to device method");
            System.out.println("Waiting for method trigger");
        }
        catch (Exception e)
        {
            System.out.println("On exception, shutting down \n" + " Cause: " + e.getCause() + " \n" +  e.getMessage());
            client.closeNow();
            System.out.println("Shutting down...");
        }

    }

    public void btnMethodOnClick(View v) throws URISyntaxException, IOException {
        Setup();
    }

    protected static class DeviceMethodStatusCallBack implements IotHubEventCallback

    {

        public void execute(IotHubStatusCode status, Object context)

        {

            System.out.println("IoT Hub responded to device method operation with status " + status.name());

        }

    }
    public void btnReceiveOnClick(View v) throws URISyntaxException, IOException
    {
        Button button = (Button) v;

        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        // IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);

        if (protocol == IotHubClientProtocol.MQTT)
        {
            MessageCallbackMqtt callback = new MessageCallbackMqtt();
            Counter counter = new Counter(0);
            client.setMessageCallback(callback, counter);
        } else
            {
            MessageCallback callback = new MessageCallback();
            Counter counter = new Counter(0);
            client.setMessageCallback(callback, counter);
        }

        try
        {
            client.open();
        } catch (Exception e2)
        {
            System.out.println("Exception while opening IoTHub connection: " + e2.toString());
        }

        try
        {
            Thread.sleep(2000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        client.closeNow();
    }

    // Our MQTT doesn't support abandon/reject, so we will only display the messaged received
    // from IoTHub and return COMPLETE
    static class MessageCallbackMqtt implements com.microsoft.azure.sdk.iot.device.MessageCallback
    {
        public IotHubMessageResult execute(Message msg, Object context)
        {
            Counter counter = (Counter) context;
            System.out.println(
                    "Received message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

            counter.increment();

            return IotHubMessageResult.COMPLETE;
        }
    }

    static class EventCallback implements IotHubEventCallback
    {
        public void execute(IotHubStatusCode status, Object context)
        {
            Integer i = (Integer) context;
            System.out.println("IoT Hub responded to message " + i.toString()
                    + " with status " + status.name());
        }
    }

    static class MessageCallback implements com.microsoft.azure.sdk.iot.device.MessageCallback
    {
        public IotHubMessageResult execute(Message msg, Object context)
        {
            Counter counter = (Counter) context;
            System.out.println(
                    "Received message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

            int switchVal = counter.get() % 3;
            IotHubMessageResult res;
            switch (switchVal)
            {
                case 0:
                    res = IotHubMessageResult.COMPLETE;
                    break;
                case 1:
                    res = IotHubMessageResult.ABANDON;
                    break;
                case 2:
                    res = IotHubMessageResult.REJECT;
                    break;
                default:
                    // should never happen.
                    throw new IllegalStateException("Invalid message result specified.");
            }

            System.out.println("Responding to message " + counter.toString() + " with " + res.name());

            counter.increment();

            return res;
        }
    }

    /**
     * Used as a counter in the message callback.
     */
    static class Counter
    {
        int num;

        Counter(int num) {
            this.num = num;
        }

        int get() {
            return this.num;
        }

        void increment() {
            this.num++;
        }

        @Override
        public String toString() {
            return Integer.toString(this.num);
        }
    }

}
