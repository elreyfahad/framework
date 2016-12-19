/**
 * Copyright (c) 2016 University of Brescia, Alessandra Flammini and Angelo Vezzoli, All rights reserved.
 *
 * @author  Angelo Vezzoli
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eu.angel.bleembedded.test;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.util.Collection;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import eu.angel.bleembedded.R;
import eu.angel.bleembedded.lib.BLEContext;
import eu.angel.bleembedded.lib.item.BLEItem;
import eu.angel.bleembedded.lib.item.alarm.BLEAlarm;
import eu.angel.bleembedded.lib.item.button.BLEButton;
import eu.angel.bleembedded.lib.item.button.BLEOnClickListener;
import eu.angel.bleembedded.lib.item.generalIO.BLEGeneralIO;
import eu.angel.bleembedded.lib.item.generalIO.BLEGeneralIOEvent;
import eu.angel.bleembedded.lib.item.generalIO.BLEOnGeneralIOEventListener;


@SuppressLint({ "NewApi", "ServiceCast" })
public class MainActivityTest2 extends Activity implements BeaconConsumer {
	
	
	protected static final String TAG = "MainActivityTest2M";
	protected static final String TAGV = "MainActivityTest2Value";
	protected final String lockCode = "12345";


	final static int LIGHT_MODE_PHOTO = 0;
	final static int LIGHT_MODE_MANUAL = 1;

	final static int VERY_DARK = 600; //sopra 700 entrambi accesi
	final static int VERY_SHINE = 100; //sotto i 100 entrambi spenti

	final static int ON=1;
	final static int OFF=0;


	int switch1status = OFF;
	int switch2status = OFF;
	int lockAbilitationStatus= OFF;


	private int photoRcycles1 = 0;
	private int photoRcycles2 = 0;
	private final int PHOTO_CYCLES = 30;
	private final int LOCK_ENABLE_ON_TIME_MS = 2000;
	//Android Embedded
	TextView tvAnalog1;
	TextView tvAnalog2;
	EditText etCode;
	Button lightModeSelector;
	Button switch1;
	Button switch2;
	Button AndroidlockEnable;
	Button checkCode;

	//TI board
	BLEButton remoteLockEnable;
	BLEAlarm wrongCodeAlarm;

	//Arduino board
	BLEGeneralIO led1A;
	BLEGeneralIO led1B;
	BLEGeneralIO led2A;
	BLEGeneralIO led2B;
	BLEGeneralIO lockEnableOut;
	BLEGeneralIO ArduinolockEnable;

	BLEGeneralIO pwmOut;

	BLEGeneralIO photoResistor1;
	BLEGeneralIO photoResistor2;

	int lightMode = LIGHT_MODE_PHOTO;


	private BeaconManager beaconManager;
	private Region region;



	private final boolean TIenabled=false;
	private final boolean arduinoLightEnabled=false;
	private final boolean arduinoLockEnabled=false;
	private final boolean arduinoAutoLightEnabled=true&arduinoLightEnabled;
	private final boolean altlibrarybeaconEnabled=false;
	private final boolean pwmEnabled=true;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maintesigm);
		tvAnalog1 = (TextView) findViewById(R.id.analogin1);
		tvAnalog2 = (TextView) findViewById(R.id.analogin2);
		etCode = (EditText) findViewById(R.id.lockCode);
		lightModeSelector = (Button) findViewById(R.id.lightModeSelector);
		switch1 = (Button) findViewById(R.id.switchRoom1);
		switch2 = (Button) findViewById(R.id.switchRoom2);
		AndroidlockEnable = (Button) findViewById(R.id.lockEnable);
		checkCode = (Button) findViewById(R.id.checkCode);


		if (lightMode == LIGHT_MODE_PHOTO) {
			switch1.setEnabled(false);
			switch2.setEnabled(false);
		} else {
			tvAnalog1.setText("MAN MODE");
			tvAnalog2.setText("MAN MODE");
		}

		BLEContext.initBLE(this);
		//context=this;


		if (TIenabled) {
			remoteLockEnable = (BLEButton) BLEContext.findViewById("lock_enable_key_button_1");
			wrongCodeAlarm = (BLEAlarm) BLEContext.getSystemService
					(BLEContext.ALARM_SERVICE, "alarm_wrong_code_key_alarm");

			remoteLockEnable.setOnClickListener(new BLEOnClickListener() {
				@Override
				public void onClick(BLEItem arg0) {
					if (arduinoLockEnabled) {
						lockEnableOut.setDigitalValueHigh(true);
						Timer timer = new Timer("stopLockEnable");
						timer.schedule(
								new TimerTask() {
									@Override
									public void run() {
										lockEnableOut.setDigitalValueHigh(false);
									}
								},
								LOCK_ENABLE_ON_TIME_MS);
					}
				}
			});
		}


		if (arduinoLightEnabled) {
			led1A = (BLEGeneralIO) BLEContext.findViewById("hac_rbs_general_io_2");
			led1B = (BLEGeneralIO) BLEContext.findViewById("hac_rbs_general_io_3");
			led2A = (BLEGeneralIO) BLEContext.findViewById("hac_rbs_general_io_4");
			led2B = (BLEGeneralIO) BLEContext.findViewById("hac_rbs_general_io_5");
		}
		if (pwmEnabled) {
		pwmOut = (BLEGeneralIO) BLEContext.findViewById("hac_rbs_general_io_6");}

		if (arduinoLockEnabled) {
			lockEnableOut = (BLEGeneralIO) BLEContext.findViewById("hac_rbs_general_io_6");
			ArduinolockEnable = (BLEGeneralIO) BLEContext.findViewById("hac_rbs_general_io_7");
		}
		if (arduinoAutoLightEnabled) {
			photoResistor1 = (BLEGeneralIO) BLEContext.findViewById("hac_rbs_general_io_14");
			photoResistor2 = (BLEGeneralIO) BLEContext.findViewById("hac_rbs_general_io_15");
		}


		lightModeSelector.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (lightMode == LIGHT_MODE_PHOTO) {
					tvAnalog1.setText("MAN MODE");
					tvAnalog2.setText("MAN MODE");
					switch1.setEnabled(true);
					switch2.setEnabled(true);
					lightMode = LIGHT_MODE_MANUAL;
					if (arduinoAutoLightEnabled) {
						photoResistor1.setStatus(BLEGeneralIO.GENERAL_IO_DI);
						photoResistor2.setStatus(BLEGeneralIO.GENERAL_IO_DI);
					}
				} else {
					switch1.setEnabled(false);
					switch2.setEnabled(false);
					if (arduinoAutoLightEnabled) {
						photoResistor1.setStatus(BLEGeneralIO.GENERAL_IO_AI);
						photoResistor2.setStatus(BLEGeneralIO.GENERAL_IO_AI);
					}
					lightMode = LIGHT_MODE_PHOTO;
				}
			}
		});

		switch1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (lightMode == LIGHT_MODE_MANUAL) {
					if (switch1status == OFF) {
						switch1status = ON;
						if (arduinoLightEnabled) {
							led1A.setDigitalValueHigh(true);
							led1B.setDigitalValueHigh(true);
						}
					} else {
						switch1status = OFF;
						if (arduinoLightEnabled) {
							led1A.setDigitalValueHigh(false);
							led1B.setDigitalValueHigh(false);
						}
					}
				}
			}
		});

		switch2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (lightMode == LIGHT_MODE_MANUAL) {
					if (switch2status == OFF) {
						switch2status = ON;
						if (arduinoLightEnabled) {
							led2A.setDigitalValueHigh(true);
							led2B.setDigitalValueHigh(true);
						}
					} else {
						switch2status = OFF;
						if (arduinoLightEnabled) {
							led2A.setDigitalValueHigh(false);
							led2B.setDigitalValueHigh(false);
						}
					}
				}
			}
		});

		AndroidlockEnable.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				lockEnableOut.setStatus(BLEGeneralIO.GENERAL_IO_SERVO);
				if (arduinoLockEnabled) {
					lockEnableOut.setDigitalValueHigh(true);
					Timer timer = new Timer("stopLockEnable");
					timer.schedule(
							new TimerTask() {
								@Override
								public void run() {
									lockEnableOut.setDigitalValueHigh(false);
								}
							},
							LOCK_ENABLE_ON_TIME_MS);
				}
			}
		});

		checkCode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String insertedCode = etCode.getText().toString();
				if (insertedCode.equals(lockCode)) {
					if (arduinoLockEnabled) {
						lockEnableOut.setDigitalValueHigh(true);
						Timer timer = new Timer("stopLockEnable");
						timer.schedule(
								new TimerTask() {
									@Override
									public void run() {
										lockEnableOut.setDigitalValueHigh(false);
									}
								},
								LOCK_ENABLE_ON_TIME_MS);
					}
				} else {
					if (TIenabled) {
						wrongCodeAlarm.alarm(5000);
					}
				}

				if (pwmEnabled)
				pwmOut.setServoValue(Float.parseFloat(insertedCode));

			}
		});

		if (arduinoAutoLightEnabled) {
			photoResistor1.setOnGeneralIOEventListener(new BLEOnGeneralIOEventListener() {
				@Override
				public void onBoardInitEnded() {
					if (lightMode == LIGHT_MODE_PHOTO)
						photoResistor1.setStatus(BLEGeneralIO.GENERAL_IO_AI);
				}

				@Override
				public void onDigitalInputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onAnalogValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {
					float fvalue = bleGeneralIOEvent.values[1];
					int value = (int) fvalue;
					Log.d(TAGV, "Analog1 value: " + Integer.toString(value));
					photoRcycles1++;
					if (photoRcycles1 > PHOTO_CYCLES) {
						final String res = String.format("%.1f", (fvalue / 1024) * 5);
						(MainActivityTest2.this).runOnUiThread(
								new Runnable() {
									@Override
									public void run() {
										tvAnalog1.setText(res + " V");
									}
								}
						);
						photoRcycles1 = 0;
						if (value < VERY_SHINE) {
							Log.d(TAGV, "room1 very shine");
							led1A.setDigitalValueHigh(false);
							led1B.setDigitalValueHigh(false);
						} else if ((VERY_SHINE < value) && (value < VERY_DARK)) {
							Log.d(TAGV, "room1 shady");
							led1A.setDigitalValueHigh(true);
							led1B.setDigitalValueHigh(false);
						} else {
							Log.d(TAGV, "room1 very dark");
							led1A.setDigitalValueHigh(true);
							led1B.setDigitalValueHigh(true);
						}
					}
				}

				@Override
				public void onDigitalOutputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onServoValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onPWMValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onGeneralIOStatusChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onSetGeneralIOParameter(BLEGeneralIOEvent bleGeneralIOEvent) {

				}
			});
			photoResistor2.setOnGeneralIOEventListener(new BLEOnGeneralIOEventListener() {
				@Override
				public void onBoardInitEnded() {
					if (lightMode == LIGHT_MODE_PHOTO)
						photoResistor2.setStatus(BLEGeneralIO.GENERAL_IO_AI);
				}

				@Override
				public void onDigitalInputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onAnalogValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {
					float fvalue = bleGeneralIOEvent.values[1];
					int value = (int) fvalue;
					Log.d(TAGV, "Analog2 value: " + Integer.toString(value));
					photoRcycles2++;
					if (photoRcycles2 > PHOTO_CYCLES) {
						final String res = String.format("%.1f", (fvalue / 1024) * 5);
						(MainActivityTest2.this).runOnUiThread(
								new Runnable() {
									@Override
									public void run() {
										tvAnalog2.setText(res + " V");
									}
								}
						);
						photoRcycles2 = 0;
						if (value < VERY_SHINE) {
							Log.d(TAGV, "room2 very shine");
							led2A.setDigitalValueHigh(false);
							led2B.setDigitalValueHigh(false);
						} else if ((VERY_SHINE < value) && (value < VERY_DARK)) {
							Log.d(TAGV, "room2 shady");
							led2A.setDigitalValueHigh(true);
							led2B.setDigitalValueHigh(false);
						} else {
							Log.d(TAGV, "room2 very dark");
							led2A.setDigitalValueHigh(true);
							led2B.setDigitalValueHigh(true);
						}
					}
				}

				@Override
				public void onDigitalOutputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onServoValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onPWMValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onGeneralIOStatusChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onSetGeneralIOParameter(BLEGeneralIOEvent bleGeneralIOEvent) {

				}
			});
		}
		if (arduinoLightEnabled) {
			led1A.setOnGeneralIOEventListener(new BLEOnGeneralIOEventListener() {
				@Override
				public void onBoardInitEnded() {
					led1A.setStatus(BLEGeneralIO.GENERAL_IO_DO);
				}

				@Override
				public void onDigitalInputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onAnalogValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {
				}

				@Override
				public void onDigitalOutputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onServoValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onPWMValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onGeneralIOStatusChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onSetGeneralIOParameter(BLEGeneralIOEvent bleGeneralIOEvent) {

				}
			});
			led1B.setOnGeneralIOEventListener(new BLEOnGeneralIOEventListener() {
				@Override
				public void onBoardInitEnded() {
					led1B.setStatus(BLEGeneralIO.GENERAL_IO_DO);
				}

				@Override
				public void onDigitalInputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onAnalogValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onDigitalOutputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onServoValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onPWMValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onGeneralIOStatusChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onSetGeneralIOParameter(BLEGeneralIOEvent bleGeneralIOEvent) {

				}
			});
			led2A.setOnGeneralIOEventListener(new BLEOnGeneralIOEventListener() {
				@Override
				public void onBoardInitEnded() {
					led2A.setStatus(BLEGeneralIO.GENERAL_IO_DO);
				}

				@Override
				public void onDigitalInputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onAnalogValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onDigitalOutputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onServoValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onPWMValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onGeneralIOStatusChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onSetGeneralIOParameter(BLEGeneralIOEvent bleGeneralIOEvent) {

				}
			});
			led2B.setOnGeneralIOEventListener(new BLEOnGeneralIOEventListener() {
				@Override
				public void onBoardInitEnded() {
					led2B.setStatus(BLEGeneralIO.GENERAL_IO_DO);
				}

				@Override
				public void onDigitalInputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onAnalogValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onDigitalOutputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onServoValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onPWMValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onGeneralIOStatusChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onSetGeneralIOParameter(BLEGeneralIOEvent bleGeneralIOEvent) {

				}
			});
		}
		if (arduinoLockEnabled) {
			lockEnableOut.setOnGeneralIOEventListener(new BLEOnGeneralIOEventListener() {
				@Override
				public void onBoardInitEnded() {
					lockEnableOut.setStatus(BLEGeneralIO.GENERAL_IO_DO);
				}

				@Override
				public void onDigitalInputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onAnalogValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onDigitalOutputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onServoValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onPWMValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onGeneralIOStatusChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onSetGeneralIOParameter(BLEGeneralIOEvent bleGeneralIOEvent) {

				}
			});
			ArduinolockEnable.setOnGeneralIOEventListener(new BLEOnGeneralIOEventListener() {
				@Override
				public void onBoardInitEnded() {
					ArduinolockEnable.setStatus(BLEGeneralIO.GENERAL_IO_DI);
				}

				@Override
				public void onDigitalInputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {
					Log.d(TAG, "digital input lock enable changed: " + bleGeneralIOEvent.values[1]);
					if (bleGeneralIOEvent.values[1] == 1) {
						lockEnableOut.setDigitalValueHigh(true);
						Timer timer = new Timer("stopLockEnable");
						timer.schedule(
								new TimerTask() {
									@Override
									public void run() {
										lockEnableOut.setDigitalValueHigh(false);
									}
								},
								LOCK_ENABLE_ON_TIME_MS);
					}
				}

				@Override
				public void onAnalogValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onDigitalOutputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onServoValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onPWMValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onGeneralIOStatusChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

				}

				@Override
				public void onSetGeneralIOParameter(BLEGeneralIOEvent bleGeneralIOEvent) {

				}
			});
		}

		if (pwmEnabled) {
		pwmOut.setOnGeneralIOEventListener(new BLEOnGeneralIOEventListener() {
			@Override
			public void onBoardInitEnded() {
				pwmOut.setStatus(BLEGeneralIO.GENERAL_IO_SERVO);
			}

			@Override
			public void onDigitalInputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

			}

			@Override
			public void onAnalogValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

			}

			@Override
			public void onDigitalOutputValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

			}

			@Override
			public void onServoValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {
				Log.d(TAG, "Servo value: " + bleGeneralIOEvent.values[1]);
			}

			@Override
			public void onPWMValueChanged(BLEGeneralIOEvent bleGeneralIOEvent) {
				Log.d(TAG, "PWM value: " + bleGeneralIOEvent.values[1]);
			}

			@Override
			public void onGeneralIOStatusChanged(BLEGeneralIOEvent bleGeneralIOEvent) {

			}

			@Override
			public void onSetGeneralIOParameter(BLEGeneralIOEvent bleGeneralIOEvent) {

			}
		});}


		if (altlibrarybeaconEnabled) {
			beaconManager = BeaconManager.getInstanceForApplication(this);

			if (beaconManager.getBeaconParsers().size() < 5) {
				// Add parser for iBeacons;
//				beaconManager.getBeaconParsers().add(new BeaconParser().
//						setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
				// Detect the Eddystone main identifier (UID) frame:
				beaconManager.getBeaconParsers().add(new BeaconParser().
						setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
				// Detect the Eddystone telemetry (TLM) frame:
				beaconManager.getBeaconParsers().add(new BeaconParser().
						setBeaconLayout("x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"));
				// Detect the Eddystone URL frame:
//				beaconManager.getBeaconParsers().add(new BeaconParser().
//						setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"));
			}

			// Get the details for all the beacons we encounter.
			region = new Region("justGiveMeEverything", null, null, null);
			if (!beaconManager.isBound(this)) {
				beaconManager.bind(this);
			}


		}
	}

	@Override
	public void onBeaconServiceConnect() {
       /*beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: "+state);
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {    } */


		beaconManager.setRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
				if (beacons.size() > 0) {
					Iterator<Beacon> beaconIterator = beacons.iterator();
					while (beaconIterator.hasNext()) {
						Beacon beacon = beaconIterator.next();
						// Debug - logging a beacon - checking background logging is working.
						logBeaconData(beacon);
					}
				}
			}
		});
		try {
			beaconManager.startRangingBeaconsInRegion(region);
		} catch (RemoteException e) {
			Log.e(TAG,e.getMessage());
		}

	}


	private void logBeaconData(Beacon beacon) {

		StringBuilder scanString = new StringBuilder();

		if (beacon.getServiceUuid() == 0xfeaa) {

			if (beacon.getBeaconTypeCode() == 0x00) {
				logGenericBeacon(scanString, beacon);
				scanString.append(" Eddystone-UID -> ");
				scanString.append(" Namespace : ").append(beacon.getId1());
				scanString.append(" Identifier : ").append(beacon.getId2());

				//logEddystoneTelemetry(scanString, beacon);

			} else if (beacon.getBeaconTypeCode() == 0x10) {
				logGenericBeacon(scanString, beacon);
				String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
				scanString.append(" Eddystone-URL -> " + url);

			} else if (beacon.getBeaconTypeCode() == 0x20) {
				logGenericBeacon(scanString, beacon);
				scanString.append(" Eddystone-TLM -> ");
				//logEddystoneTelemetry(scanString, beacon);

			}

		} else {

			// Just an old fashioned iBeacon or AltBeacon...
			logGenericBeacon(scanString, beacon);

		}

		Log.d(TAG,scanString.toString());
		System.out.println(scanString.toString());

		//logToDisplay(scanString.toString());
		//scanString.append("\n");

	}

	/**
	 * Logs iBeacon & AltBeacon data.
	 */
	private void logGenericBeacon(StringBuilder scanString, Beacon beacon) {
		scanString.append(" UUID: ").append(beacon.getId1());
        /*scanString.append(" Maj. Mnr.: ");
        if (beacon.getId2() != null) {
            scanString.append(beacon.getId2());
        }
        scanString.append("-");
        if (beacon.getId3() != null) {
            scanString.append(beacon.getId3());
        }*/

		scanString.append(" RSSI: ").append(beacon.getRssi());
		scanString.append(" Proximity: ").append(BeaconHelper.getProximityString(beacon.getDistance()));
		scanString.append(" Power: ").append(beacon.getTxPower());
		//scanString.append(" Timestamp: ").append(BeaconHelper.getCurrentTimeStamp());
	}



}