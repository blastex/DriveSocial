using Microsoft.Azure.Devices.Client;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace CameraCapture
{
    public class Device
    {
        private DeviceClient s_deviceClient;

        // The device connection string to authenticate the device with your IoT hub.
        // Using the Azure CLI:
        // az iot hub device-identity show-connection-string --hub-name {YourIoTHubName} --device-id MyDotnetDevice --output table
        //private readonly static string s_connectionString = "HostName=drivesocialhub.azure-devices.net;DeviceId=pc001;SharedAccessKey=iYAWYFxL8D62MXYdTjj9FIiMkmYd0PJOUfb8P/5k30k=";
        private readonly static string s_connectionString = "HostName=drivesocialhub.azure-devices.net;DeviceId=raspi001;SharedAccessKey=NzKxtH8VfEKNGBtRP4Na0IzgjXRQMrscrcrwIlW0kmA=";

        public Device()
        {
            // Connect to the IoT hub using the MQTT protocol
            s_deviceClient = DeviceClient.CreateFromConnectionString(s_connectionString, TransportType.Mqtt);
        }

        // Async method to send simulated telemetry
        public async void SendDeviceToCloudMessagesAsync(string data, bool found)
        {
            // Initial telemetry values
            double minTemperature = 20;
            double minHumidity = 60;
            Random rand = new Random();

            double currentTemperature = minTemperature + rand.NextDouble() * 15;
            double currentHumidity = minHumidity + rand.NextDouble() * 20;

            // Create JSON message
            //var telemetryDataPoint = new
            //{
            //    temperature = currentTemperature,
            //    humidity = currentHumidity
            //};
            Message message;
            if (data != null)
            {
                var messageString = data;
                message = new Message(Encoding.ASCII.GetBytes(messageString));
            }
            else
            {
                message = new Message();
            }

            // Add a custom application property to the message.
            // An IoT hub can filter on these properties without access to the message body.
            message.Properties.Add("found", found ? "true" : "false");

            // Send the tlemetry message
            await s_deviceClient.SendEventAsync(message);

        }
    }
}
