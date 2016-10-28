package thermapp.sdk.sample;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import thermapp.sdk.MeasurementData;
import thermapp.sdk.ThermAppAPI;
import thermapp.sdk.ThermAppAPI.Coloring_Mode;
import thermapp.sdk.ThermAppAPI.Measurement_Mode;
import thermapp.sdk.ThermAppAPI.Mode;
import thermapp.sdk.ThermAppAPI_Callback;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;

import thermapp.sdk.sample.R;

public class MainActivity extends Activity implements ThermAppAPI_Callback 
{

	private ThermAppAPI mDeviceSdk = null;
	private Bitmap bmp_ptr = null;

	int[] gray_palette;
	int[] therm_palette;
	int[] my_palette;
	Matrix matrix_imrot_90;

	private Runnable rnbl = new Runnable() 
	{
		public void run() 
		{
			iv.setImageBitmap(Bitmap.createBitmap(bmp_ptr, 0, 0,
					bmp_ptr.getWidth(), bmp_ptr.getHeight(), matrix_imrot_90,
					true));
		}
	};
	private static final String media_path = Environment
			.getExternalStorageDirectory().getPath() + "/ThermApp/Media";

	private TextView tv_temp = null;
	private TextView small1 = null;
	private TextView small2 = null;
	private TextView hypelinked = null;
	private ImageView iv = null;
	private ImageButton settings = null;

	private float[] CtoF;
	private String FerCel;

	private SharedPreferences prefs;
	private RelativeLayout relay_temp_cross;
	private ImageView imtst;
	private RelativeLayout Splash_Lay;
	private RelativeLayout NoCam_Lay;
	private ImageView barBck;

	
	/* OUR CODE */
	private Button compose;
	private Button startOrStopSaveFrameRawData;
	String timeStamp;
	@SuppressLint("UseSparseArrays") LinkedHashMap<Integer, int[]> hmap = new LinkedHashMap<Integer, int[]>();
	int frameIndex=0;
	private boolean isToSaveFrameRawData = false;
	
	@Override
	public void onStart() 
	{
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() 
	{
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public void OnFrameGetThermAppBMP(Bitmap bmp, int[] iMinMaxThresholdValues) 
	{
		if (null != bmp) 
		{
			bmp_ptr = bmp;
			iv.post(rnbl);
		}
		
		/*
		 * Every callback the minimum and maximum temperatures in the frame are received.
		 * Temperatures units are integers but treated as floats – therefore their values should be [Temperature] * 100.
		 * For example, 25.70 degrees is received as 2570.
		float min_temperature = (float)(iMinMaxThresholdValues[0]);
		float max_temperature = (float)(iMinMaxThresholdValues[1]);
		int threshold = iMinMaxThresholdValues[2];	// Grayscale switching point between B&W and color, range is [0-255].
		*/
		

	}

	@Override
	public void OnFrameGetThermAppTemperatures(final int[] frame, final int w, final int h, MeasurementData measurementData) 
	{
		final float central_pix = (float) (frame[(w >> 1) * (h + 1)]) / 100
				* CtoF[0] + CtoF[1];

		tv_temp.post(new Runnable() 
		{
			public void run() 
			{
				DecimalFormat form = new DecimalFormat("#,##00.0°" + FerCel);
				tv_temp.setText(form.format(central_pix));
				if(isToSaveFrameRawData)
					hmap.put(frameIndex++, frame);
			}
		});

		/*
		 * On ThermApp-TH devices, when measurement mode is set to AREA/LINE/HILO, the following data fields contain:
		 * 	measurementData.fMin	- minimum temperature at the current measurement mode
		 *	measurementData.fMax	- maximum temperature at the current measurement mode 
		 *	measurementData.fAvg	- average temperature at the current measurement mode 
		 *	measurementData.pMin	- IR coordinate of the minimum temperature pixel at the current measurement mode 
		 *	measurementData.pMax	- IR coordinate of the maximum temperature pixel at the current measurement mode 
		 * 
		 * On Other measurement modes, or on Non-TH devices, the above fields will have value of 0.

		MeasurementData md = measurementData;
		if (mDeviceSdk.IsThDevice() && measurementData.fMax != 0)
		{
			Log.d("OnFrameGetThermAppTemperatures","Measurement mode data: Min=" + md.fMin + " @ (" + md.pMin.x + "," + md.pMin.y + ") , Max=" + md.fMax + " @ (" + md.pMax.x + "," + md.pMax.y +") , Avg=" + md.fAvg);
		}
		*/
	}

	@Override
	public void onPause() 
	{
		super.onPause();
	}

	@Override
	public void onDestroy() 
	{
		if (this.isFinishing())
			CloseApp();
	}

	@Override
	public void onResume() 
	{
				
		if (prefs.getString("listMode", "none").equals("2")) // Thermography mode
		{
			// Turn everything on
			small1.setVisibility(View.VISIBLE);
			small2.setVisibility(View.VISIBLE);
			imtst.setVisibility(View.VISIBLE);
			barBck.setImageResource(R.drawable.night_vision_nt);
			relay_temp_cross.setVisibility(View.VISIBLE);

			if (prefs.getString("listpalette", "none").equals("1"))
				SetThermalMode_Col();

			else if (prefs.getString("listpalette", "none").equals("2"))
				SetThermalMode_BW();

			else if (prefs.getString("listpalette", "none").equals("3"))
				SetThermalMode_Purp();
		}
		else // NightVision mode
		{ 
			SetEnhancedMode();
			relay_temp_cross.setVisibility(View.GONE);
			small1.setVisibility(View.INVISIBLE);
			small2.setVisibility(View.INVISIBLE);
			imtst.setVisibility(View.INVISIBLE);
			barBck.setImageResource(R.drawable.night_vision_t);
		}
		
		if (prefs.getString("listUnits", "C").equals("C")) 
		{
			CtoF[0] = 1;
			CtoF[1] = 0;
			FerCel = "C";
		}
		else if (prefs.getString("listUnits", "C").equals("F")) 
		{
			CtoF[0] = 1.8f;
			CtoF[1] = 32;
			FerCel = "F";
		}
		
		if (null != mDeviceSdk)
		{
			if(!mDeviceSdk.SetIgnoringRatio(0.25f/100f))	// Set the ignoring ratio (Scale Truncation) value to 0.25%
			{
				Log.d("SetIgnoringRatio","Please enter valid value" );
			}
		}
		super.onResume();
	}

	private void CloseApp() 
	{
		if (null != mDeviceSdk)
		{
			mDeviceSdk.Close();
		}
		this.finish();
		System.exit(0);
	}

	private boolean InitSdk() 
	{
		// Create Developer SDK Instance
		mDeviceSdk = new ThermAppAPI(this);

		// Try to open usb interface
		try 
		{
			mDeviceSdk.ConnectToDevice();
		}
		catch (Exception e) 
		{
			// Close SDK
			mDeviceSdk = null;
			return false;
		}
		return true;
	}

	private void SetEnhancedMode() 
	{

		try 
		{
			if (null != mDeviceSdk)
				mDeviceSdk.SetMode(Mode.Enhanced, gray_palette, Coloring_Mode.Normal);
		}
		catch (Exception e) {}

		Bitmap bm = Bitmap.createBitmap(gray_palette, 256, 1,
				Bitmap.Config.ARGB_8888);
		imtst.setImageBitmap(bm);
	}

	private void SetThermalMode_Col() 
	{
		try 
		{
			if (null != mDeviceSdk)
				mDeviceSdk.SetMode(Mode.Thermography, therm_palette, Coloring_Mode.Normal);
		}
		catch (Exception e) {}

		Bitmap bm = Bitmap.createBitmap(therm_palette, 256, 1,
				Bitmap.Config.ARGB_8888);
		imtst.setImageBitmap(bm);
	}

	private void SetThermalMode_BW() 
	{
		try 
		{
			if (null != mDeviceSdk)
				mDeviceSdk.SetMode(Mode.Thermography, gray_palette, Coloring_Mode.Normal);
		}
		catch (Exception e) {}

		Bitmap bm = Bitmap.createBitmap(gray_palette, 256, 1,
				Bitmap.Config.ARGB_8888);
		imtst.setImageBitmap(bm);
	}

	private void SetThermalMode_Purp()
	{
		try 
		{
			if (null != mDeviceSdk)
				mDeviceSdk.SetMode(Mode.Thermography, my_palette, Coloring_Mode.Normal);
		}
		catch (Exception e) {}

		Bitmap bm = Bitmap.createBitmap(my_palette, 256, 1,
				Bitmap.Config.ARGB_8888);
		imtst.setImageBitmap(bm);
	}

	private int[] createPalette(int sR, int sG, int sB, int eR, int eG, int eB) 
	{
		int[] my_palette = new int[256];
		float pr;
		float Red;
		float Green;
		float Blue;

		for (int i = 0; i < 256; i++) 
		{
			pr = (float) i / (float) 256;
			Red = sR * pr + eR * (1 - pr);
			Green = sG * pr + eG * (1 - pr);
			Blue = sB * pr + eB * (1 - pr);

			my_palette[i] = 0xFF000000 | (Math.round(Red) << 16)
					| (Math.round(Green) << 8) | (Math.round(Blue) << 0);
		}
		return my_palette;
	}

	private void addListenerOnButtons() 
	{
		iv = (ImageView) findViewById(R.id.imageView1);

		tv_temp = (TextView) findViewById(R.id.textView_temp);
		relay_temp_cross = (RelativeLayout) findViewById(R.id.Rel_Temp_Cross);

		small1 = (TextView) findViewById(R.id.small1);
		small2 = (TextView) findViewById(R.id.small2);
		hypelinked = (TextView) findViewById(R.id.hyperlikedText);

		imtst = (ImageView) findViewById(R.id.imageView_tst);
		Splash_Lay = (RelativeLayout) findViewById(R.id.Rel_Splash);
		Splash_Lay.setVisibility(View.VISIBLE);

		NoCam_Lay = (RelativeLayout) findViewById(R.id.Rel_Nocam);
		barBck = (ImageView) findViewById(R.id.imageView_barBck);

		CtoF = new float[2];

		hypelinked.setClickable(true);
		hypelinked.setMovementMethod(LinkMovementMethod.getInstance());
		String text = "<a href='http://www.therm-app.com'>www.therm-app.com</a>";
		hypelinked.setText(Html.fromHtml(text));

		settings = (ImageButton) findViewById(R.id.setting_btn);
		settings.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Intent in = new Intent(MainActivity.this, SettingsActivity.class);
				in.putExtra("serialnum", Integer.toString(mDeviceSdk.GetSerialNumber()));
				startActivity(in);
			}
		});
		
		/* OUR CODE */
		compose = (Button) findViewById(R.id.printScreen);
		compose.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{       
				Date date= new Date();
				timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date.getTime());
		    	File root = new File(media_path, "RawData/" + timeStamp);
		        if (!root.exists()) {
		            root.mkdirs();
		        }
		        File gpxfile;
				for(Entry<Integer, int[]> entry : hmap.entrySet()) {
				    int[] frame = entry.getValue();
				    gpxfile = new File(root, entry.getKey().toString());
				    generateNoteOnSD(gpxfile, frame);
				}
				frameIndex=0;
				hmap.clear();
			}
		});
		
		startOrStopSaveFrameRawData = (Button) findViewById(R.id.StartOrStopSaveFrameRawData);
		startOrStopSaveFrameRawData.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{   
				isToSaveFrameRawData = !isToSaveFrameRawData;
				if(isToSaveFrameRawData)
					startOrStopSaveFrameRawData.setText("Stop");
				else
					startOrStopSaveFrameRawData.setText("Start");
			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) 
			{
				String result = data.getStringExtra("result");

				if (result.equals("OK")) 
				{
					try 
					{
						mDeviceSdk.StartVideo();
						mDeviceSdk.SetTmaxTmin(mDeviceSdk.ciAUTO_TEMP_INDICATOR, mDeviceSdk.ciAUTO_TEMP_INDICATOR);	// Sets minimum and maximum temperature to be in automatic mode
					} 
					catch (Exception e) 
					{
						// Report error to use
						AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
						dlgAlert.setMessage("Unable to start video: " + e.getMessage());
						dlgAlert.setTitle("ThermApp");
						dlgAlert.setPositiveButton("OK", null);
						dlgAlert.setCancelable(true);
						dlgAlert.create().show();
					}
					
					Splash_Lay.setVisibility(View.GONE);
				}
				else if (result.equals("EXIT")) 
				{
					CloseApp();
				}
			}
			if (resultCode == RESULT_CANCELED) 
			{
				CloseApp();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		addListenerOnButtons();

		matrix_imrot_90 = new Matrix();
		matrix_imrot_90.postRotate(90);

		CreatePalettes();
		prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

		// Initializes ThermApp SDK
		if (InitSdk() && mDeviceSdk.IsValidSerial()) 
		{
			try 
			{
				// Start Image Processing
				mDeviceSdk.AfterConnect();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "Unable to start image processing!", Toast.LENGTH_LONG).show();
			}
			
			if (mDeviceSdk.IsThDevice())
			{
				Toast.makeText(getApplicationContext(), "ThermApp-TH device detected!", Toast.LENGTH_LONG).show();

				// Switch to Thermography mode if TH device is connected
				prefs.edit().putString("listMode", "2").commit();		// Set Thermography mode
				prefs.edit().putString("listpalette", "1").commit();	// Set Rainbow palette
				
				SetThermalMode_Col();	// Change mode

				/*
				 * On ThermApp-TH devices, you can change the measurement to AREA/LINE/HILO.
				 * The following example requests the API to report back (at the OnFrameGetThermAppTemperatures callback) the minimum and maximum temperatures 
				 * of the entire frame, their coordinates and average.
				 * Note: Bad pixels on the measured area can produce bad results. 

				Point point_start = new Point(0,mDeviceSdk.GetHeight()-1);		// start measurement point (top left of the frame considering portrait mode)
				Point point_end = new Point(mDeviceSdk.GetWidth()-1,0);	// end measurement point (bottom right of the frame considering portrait mode)
				try 
				{
					mDeviceSdk.SetMeasurementMode(Measurement_Mode.HILO, point_start, point_end);
				}
				catch (Exception e) 
				{
					Log.e("SetMeasurementMode()","Measurement points are possibly invalid"); 
				}
				 */				
			}
			
			final Intent i = new Intent(this, WelcomeActivity.class);
			i.putExtra("serialnum", Integer.toString(mDeviceSdk.GetSerialNumber()));
			startActivityForResult(i, 1);
		}
		else 
		{
			NoCam_Lay.setVisibility(View.VISIBLE);
		}

		// Define USB detached event receiver
		BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
		{
			public void onReceive(Context context, Intent intent) 
			{
				if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction()))
					CloseApp();
			}
		};

		// Listen for new devices
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		File folder = new File(media_path);
		if (!folder.exists())
			folder.mkdir();
	}
		
	@Override
	public void onBackPressed() 
	{
		CloseApp();
		super.onBackPressed();
	}

	@Override
	protected void onNewIntent(Intent intent) 
	{

		if (Splash_Lay.getVisibility() == View.VISIBLE) 
		{
			onCreate(new Bundle());
		}
		super.onNewIntent(intent);
	}
	
	private void CreatePalettes() 
	{
		gray_palette = new int[256];
		for (int i = 0; i < 256; i++)
			gray_palette[i] = 0xFF000000 | (i << 0) | (i << 8) | (i << 16);

		int PALETTE_MAX_IND = 256 - 1;
		therm_palette = new int[256];

		therm_palette[0]= 0xFF000083;
		for (int i=1 ; i<32 ; i++)
		{
			therm_palette[i]= therm_palette[i-1]+4;
		}
		therm_palette[32]= 0xFF0004ff;
		for (int i=33; i<95 ;i++)	
		{
			therm_palette[i]= therm_palette[i-1]+0x400;
		}
		therm_palette[95]= 0xFF00ffff;
		therm_palette[96]= 0xFF04fffb;
		for (int i=97; i<159 ;i++)	
		{
			therm_palette[i]= therm_palette[i-1]+0x3FFFC;
		}
		therm_palette[159]= 0xFFffff00;
		for (int i=160; i<223 ;i++)	
		{
			therm_palette[i]= therm_palette[i-1]-0x400;
		}
		therm_palette[223]=0xFFfb0000 ;
	
		for (int i=224; i<256 ;i++)	
		{
			therm_palette[i]= therm_palette[i-1]-0x40000;
		}	

		my_palette = createPalette(253, 250, 0, 86, 0, 154);
	}
	
	/* OUR CODE */
	public void generateNoteOnSD(File gpxfile, final int[] frame) {
	    try {
	        final StringBuilder sb = new StringBuilder();
	        
	        for (int degrees : frame)
    	        {
    	            sb.append(degrees);
    	            sb.append("\n");
    	        }

	        FileWriter writer = new FileWriter(gpxfile);
	        writer.append(sb.toString());
	        writer.flush();
	        writer.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

}