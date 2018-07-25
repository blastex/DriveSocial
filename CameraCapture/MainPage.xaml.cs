using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Windows.Devices.Enumeration;                    // For DeviceInformation, DeviceClass.
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.Graphics.Imaging;
using Windows.Media.Capture;                          // For MediaCapture.
using Windows.Media.Devices;                          // For FocusSettings, FocusMode, AutoFocusRange.
using Windows.Media.MediaProperties;                  // For ImageEncodingProperties.
using Windows.Storage;                                // For StorageFile.
using Windows.Storage.Streams;
using Windows.UI.Popups;                              // For MessageDialog().
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;


// The Blank Page item template is documented at https://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x409

namespace CameraCapture
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        private MediaCapture captureMgr { get; set; }
        // Replace <Subscription Key> with your valid subscription key.
        const string subscriptionKey = "8249dae738104004addad3c4ae70bd75";
        const string uriBase =
            "https://southeastasia.api.cognitive.microsoft.com/vision/v1.0/ocr";
        private Device device = new Device();

        public MainPage()
        {
            this.InitializeComponent();
        }

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            this.lblMsg.Text = "";

            // Create a new instance of MediaCapture and start the camera in preview mode.
            this.InitCapture();
        }

        //private async void btnCapture_OnClick(object sender, RoutedEventArgs e)
        //{
        //    // Acknowledge that the user has triggered a button to capture a barcode.
        //    this.lblMsg.Text = "-------";

        //    // Capture the photo from the camera to a storage-file.
        //    ImageEncodingProperties fmtImage = ImageEncodingProperties.CreateJpeg();
        //    StorageLibrary libPhoto = await StorageLibrary.GetLibraryAsync(KnownLibraryId.Pictures);
        //    StorageFile storefile =
        //       await libPhoto.SaveFolder.CreateFileAsync("BarcodePhoto.jpg",
        //                                                  CreationCollisionOption.ReplaceExisting);
        //    await this.captureMgr.CapturePhotoToStorageFileAsync(fmtImage, storefile);

        //    // Tell the user that we have taken a picture.
        //    this.lblMsg.Text = "Picture taken";
        //}

        /// <summary>
        /// uses memory stream
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private async void btnCapture_OnClick(object sender, RoutedEventArgs e)
        {
            // Acknowledge that the user has triggered a button to capture a barcode.
            this.lblMsg.Text = "-------";

            // Capture the photo from the camera to a storage-file.
            ImageEncodingProperties fmtImage = ImageEncodingProperties.CreateBmp();
            byte[] bytes;
            using (var captureStream = new InMemoryRandomAccessStream())
            {
                await this.captureMgr.CapturePhotoToStreamAsync(fmtImage, captureStream);
                captureStream.Seek(0);
                bytes = new byte[captureStream.Size];
                await captureStream.ReadAsync(bytes.AsBuffer(), (uint)captureStream.Size, InputStreamOptions.None);
                //var decoder = await BitmapDecoder.CreateAsync(captureStream);
                //var pixelData = await decoder.GetPixelDataAsync();
                //bytes = pixelData.DetachPixelData();
            }

            var response = await MakeOCRRequest(bytes);
            bool found = false;
            if (response["regions"] != null && ((JArray)response["regions"]).Count > 0)
            {
                var region = ((JArray)response["regions"])[0];
                if (region["lines"] != null && ((JArray)region["lines"]).Count > 0)
                {
                    var line = ((JArray)region["lines"])[0];
                    if (line["words"] != null && ((JArray)line["words"]).Count > 0)
                    {
                        var word = ((JArray)line["words"])[0];
                        found = true;
                        var data = new
                        {
                            found,
                            word
                        };
                        this.device.SendDeviceToCloudMessagesAsync(JsonConvert.SerializeObject(data), found);
                        // Tell the user that we have taken a picture.
                        this.lblMsg.Text = Regex.Replace(JsonConvert.SerializeObject(word), @"\t|\n|\r|\s", "");
                    }
                }
            }
            if (!found)
            {
                var data = new
                {
                    found = false,
                    response
                };
                this.device.SendDeviceToCloudMessagesAsync(JsonConvert.SerializeObject(data), false);
                this.lblMsg.Text = Regex.Replace(response.ToString(), @"\t|\n|\r|\s", "");
            }

            // Tell the user that we have taken a picture.
        }

        /// <summary>
        /// Gets the text visible in the specified image file by using
        /// the Computer Vision REST API.
        /// </summary>
        /// <param name="imageFilePath">The image file with printed text.</param>
        static async Task<JToken> MakeOCRRequest(byte[] byteData)
        {
            try
            {
                HttpClient client = new HttpClient();

                // Request headers.
                client.DefaultRequestHeaders.Add(
                    "Ocp-Apim-Subscription-Key", subscriptionKey);

                // Request parameters.
                string requestParameters = "language=unk&detectOrientation=true";

                // Assemble the URI for the REST API Call.
                string uri = uriBase + "?" + requestParameters;

                HttpResponseMessage response;
                
                using (ByteArrayContent content = new ByteArrayContent(byteData))
                {
                    // This example uses content type "application/octet-stream".
                    // The other content types you can use are "application/json"
                    // and "multipart/form-data".
                    content.Headers.ContentType =
                        new MediaTypeHeaderValue("application/octet-stream");

                    // Make the REST API call.
                    response = await client.PostAsync(uri, content);
                }

                // Get the JSON response.
                string contentString = await response.Content.ReadAsStringAsync();
                
                // Display the JSON response.
                return JToken.Parse(contentString);
            }
            catch (Exception e)
            {
                return e.Message;
            }
        }

        private void btnTerminateApp_OnClick(object sender, RoutedEventArgs e)
        {
            this.ReleaseCapture();
            Application.Current.Exit();
        }

        private async void InitCapture()
        // Initialize everything about MediaCapture.
        {
            this.captureMgr = new MediaCapture();
            await this.captureMgr.InitializeAsync();

            // Use the lowest resolution in order to speed up the process.
            this.SetCaptureElementToMinResolution();

            // Start the camera preview.
            captureElement.Source = this.captureMgr;
            await this.captureMgr.StartPreviewAsync();

            // Ask the camera to auto-focus now.
            var focusControl = this.captureMgr.VideoDeviceController.FocusControl;
            //var modes = focusControl.SupportedFocusModes;
            //var settings = new FocusSettings
            //{
            //    Mode = FocusMode.Auto,
            //    //Value = 100,
            //    //AutoFocusRange = AutoFocusRange.FullRange,
            //    DisableDriverFallback = true
            //};
            //focusControl.Configure(settings);
            //await focusControl.FocusAsync();

            #region Error handling
            MediaCaptureFailedEventHandler handler = (sender, e) =>
            {
                System.Threading.Tasks.Task task = System.Threading.Tasks.Task.Run(async () =>
                {
                    await new MessageDialog("There was an error capturing the video from camera.", "Error").ShowAsync();
                });
            };

            this.captureMgr.Failed += handler;
            #endregion
        }

        private async void SetCaptureElementToMinResolution()
        // Set the CaptureElement to the minimum resolution that the device
        // can handle. We need to set it to lowest resolution in other to
        // speed up the process.
        {
            VideoEncodingProperties resolutionMin = null;
            int nMinResolutionSoFar = -1;
            var lisResolution = this.captureMgr.VideoDeviceController.GetAvailableMediaStreamProperties(MediaStreamType.Photo);

            for (var i = 0; i < lisResolution.Count; i++)
            {
                VideoEncodingProperties resCur = (VideoEncodingProperties)lisResolution[i];
                int nCurResolution = (int)(resCur.Width * resCur.Height);
                if (nMinResolutionSoFar == -1)
                {
                    nMinResolutionSoFar = nCurResolution;
                    resolutionMin = resCur;
                }
                else if (nMinResolutionSoFar > nCurResolution)
                {
                    nMinResolutionSoFar = nCurResolution;
                    resolutionMin = resCur;
                }
            }

            await this.captureMgr.VideoDeviceController.SetMediaStreamPropertiesAsync(MediaStreamType.Photo, resolutionMin);
        }

        private async void ReleaseCapture()
        // Release the resources used for capturing photo.
        {
            try
            {
                captureElement.Source = null;
                await this.captureMgr.StopPreviewAsync();
                this.captureMgr.Dispose();
            }
            catch (Exception ex)
            {
                String sErrMsg = String.Concat("Fail to release resources related to the ",
                                                "use of the camera. The error message is: ",
                                                ex.Message);
                await new MessageDialog(sErrMsg, "Error").ShowAsync();
            }
        }


    }

}
