package it.unibs.sandroide.lib.beacon.msg;
/**
 * Copyright (c) 2016 University of Brescia, Alessandra Flammini, All rights reserved.
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

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BeaconMsgNearable extends BeaconMsgBase {

    public static String MSG_LAYOUT = "m:0-1=5d01,i:2-11,d:13-13,d:14-14,d:15-15,d:16-16,d:17-17,d:18-18,d:19-19,d:20-20,d:21-21,p:21-21";
    public static String MSG_TYPECODE = "5d01";

    public double mTemperature=-1;
    public double[] mAcceleration;
    public boolean mIsMoving=false;
    public long currentMotionStateDuration;
    public long lastMotionStateDuration;
    public BatteryLevel batteryLevel;
    public BroadcastingPower power;

    public BeaconMsgNearable(Beacon b) {
        super(b);
    }

    @Override
    public BeaconMsgBase parse() {
        if (!this.MSG_TYPECODE.toUpperCase().equals(Long.toHexString(this.getBeaconTypeCode()).toUpperCase())) {
            return null;
        }
        // Note: Here you can put your code to parse message data fields into convenient instance class variables

        byte[] bytes=new byte[20];

        // by creating following byte array, the parsing function can be left exactly the same as those in NearableUtils.class
        //bytes[11] = (byte)this.getDataFields().get(1).longValue();  //temp
        //bytes[12] =  49;  //
        bytes[11] = (byte)this.getDataFields().get(0).longValue();  //temp
        bytes[12] = (byte)this.getDataFields().get(1).longValue();  //temp
        bytes[13] = (byte)this.getDataFields().get(2).longValue();  //ismoving
        bytes[14] = (byte)this.getDataFields().get(3).longValue();  //accellX
        bytes[15] = (byte)this.getDataFields().get(4).longValue();  //accellY
        bytes[16] = (byte)this.getDataFields().get(5).longValue();  //accellZ
        bytes[17] = (byte)this.getDataFields().get(6).longValue();  //CurrentMotionStateDuration
        bytes[18] = (byte)this.getDataFields().get(7).longValue();  //LastMotionStateDuration
        bytes[19] = (byte)this.getDataFields().get(8).longValue();  //power

        mTemperature = parseTemperature(bytes);
        currentMotionStateDuration = parseMotionStateDuration(bytes[13]);
        mAcceleration = new double[] {
                parseAcceleration(bytes[14]),
                parseAcceleration(bytes[15]),
                parseAcceleration(bytes[16])
        };
        currentMotionStateDuration = parseMotionStateDuration(bytes[17]);
        lastMotionStateDuration = parseMotionStateDuration(bytes[18]);
        batteryLevel = batteryLevel(parseIdleBatteryVoltage(bytes), parseStressBatteryVoltage(bytes));
        power = parseBroadcastingPower(bytes[19]);


        return this;
    }

    public double getAccellX() {
        return mAcceleration[0];
    }

    public double getAccellY() {
        return mAcceleration[1];
    }

    public double getAccellZ() {
        return mAcceleration[2];
    }

    public double getTemp() {
        return mTemperature;
    }



    // this parsing function has been taken by NearableUtils.class file of estimote sdk binary decompiled by Android Studio
    private static double parseTemperature(byte[] bytes) {
        // instead of 49 the following should be (byte)longs.get(0).longValue() but that doesn't give correct temp values sometimes!
        //byte[] bytes = new byte[]{(byte)longs.get(1).longValue(),49};

        int temperatureRaw = (((bytes[12] & 255) << 8) + (bytes[11] & 255) & 4095) << 4;

        double realTemperature;
        if((temperatureRaw & '耀') == 0) {
            realTemperature = (double)(temperatureRaw & '\uffff') / 256.0D;
            return Math.ceil(realTemperature * 1000.0D) / 1000.0D;
        } else {
            realTemperature = (double)((temperatureRaw & 32767) - '耀') / 256.0D;
            return Math.floor(realTemperature * 1000.0D) / 1000.0D;
        }
    }

    // this parsing function has been taken by NearableUtils.class file of estimote sdk binary decompiled by Android Studio
    private static double parseAcceleration(byte accelerationByte) {
        return (double)accelerationByte * 15.625D;
    }

    private static double parseIdleBatteryVoltage(byte[] bytes) {
        return (bytes[1] & 128) != 0?0.0D:countRealVoltage(bytes);
    }

    private static double parseStressBatteryVoltage(byte[] bytes) {
        return (bytes[1] & 128) == 0?0.0D:countRealVoltage(bytes);
    }

    private static double countRealVoltage(byte[] bytes) {
        int bat = ((bytes[1] & 255) << 8) + (bytes[0] & 255) >>> 4 & 1023;
        double realVoltage = 3.5999999999999996D * (double)bat / 1023.0D;
        return (double)Math.round(realVoltage * 1000.0D) / 1000.0D;
    }

    private static BatteryLevel batteryLevel(double idleBatteryVoltage, double stressBatteryVoltage) {
        return idleBatteryVoltage >= 2.95D?BatteryLevel.HIGH:(idleBatteryVoltage < 2.95D && idleBatteryVoltage >= 2.7D?BatteryLevel.MEDIUM:(idleBatteryVoltage > 0.0D?BatteryLevel.LOW:BatteryLevel.UNKNOWN));
    }

    private static FirmwareState parseFirmwareState(byte input) {
        boolean isApp = (input & 64) == 64;
        return isApp?FirmwareState.APP:FirmwareState.BOOTLOADER;
    }

    private static String parseHardwareVersion(byte hardwareId) {
        switch(hardwareId) {
            case 1:
                return "D3.2";
            case 2:
                return "D3.3";
            case 3:
                return "D3.4";
            case 4:
                return "SB0";
            default:
                return "Unknown";
        }
    }

    private static String parseFirmwareVersion(byte input) {
        switch(input) {
            case -127:
                return "SA1.0.0";
            case -126:
                return "SA1.0.1";
            default:
                return "Unknown";
        }
    }

    private static String parseBootloaderVersion(byte input) {
        switch(input) {
            case 1:
                return "SB1.0.0";
            default:
                return "Unknown";
        }
    }

    private static BroadcastingPower parseBroadcastingPower(byte input) {
        byte value = (byte)(input & 15);
        switch(value) {
            case 0:
                return BroadcastingPower.LEVEL_1;
            case 1:
                return BroadcastingPower.LEVEL_2;
            case 2:
                return BroadcastingPower.LEVEL_3;
            case 3:
                return BroadcastingPower.LEVEL_7;
            case 4:
                return BroadcastingPower.LEVEL_5;
            case 5:
                return BroadcastingPower.LEVEL_6;
            case 6:
                return BroadcastingPower.LEVEL_4;
            case 7:
                return BroadcastingPower.LEVEL_8;
            default:
                return BroadcastingPower.LEVEL_4;
        }
    }

    private static long parseMotionStateDuration(byte input) {
        byte unit = (byte)(input >>> 6 & 3);
        byte duration = (byte)(input & 63);
        switch(unit) {
            case 0:
                return (long)duration;
            case 1:
                return TimeUnit.MINUTES.toSeconds((long)duration);
            case 2:
                return TimeUnit.HOURS.toSeconds((long)duration);
            default:
                return duration < 32?TimeUnit.DAYS.toSeconds((long)duration):7L * TimeUnit.DAYS.toSeconds((long)(duration - 32));
        }
    }

    private static boolean parseMotionState(byte motionStateByte) {
        return (motionStateByte & 64) == 64;
    }

    public String toString() {
        return String.format("%s,\nEXTRA:temp:%s,\nax:%s,\nay:%s,\naz:%s",super.toString(),mTemperature,mAcceleration[0],mAcceleration[1],mAcceleration[2]);
    }

    @Override
    public String getImage() {
        return BASE64_IMAGE;
    }


    private static String BASE64_IMAGE = "iVBORw0KGgoAAAANSUhEUgAAAIAAAACCCAYAAACO9sDAAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyJpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMy1jMDExIDY2LjE0NTY2MSwgMjAxMi8wMi8wNi0xNDo1NjoyNyAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENTNiAoV2luZG93cykiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6RENBNEQ2OTVFNkZFMTFFNjk2MDNFOTgzNUI0NjJGNTkiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6RENBNEQ2OTZFNkZFMTFFNjk2MDNFOTgzNUI0NjJGNTkiPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW5zdGFuY2VJRD0ieG1wLmlpZDpEQ0E0RDY5M0U2RkUxMUU2OTYwM0U5ODM1QjQ2MkY1OSIgc3RSZWY6ZG9jdW1lbnRJRD0ieG1wLmRpZDpEQ0E0RDY5NEU2RkUxMUU2OTYwM0U5ODM1QjQ2MkY1OSIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/Pos4sokAADb3SURBVHja7H0JmGVF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";



    protected enum BroadcastingPower {
        LEVEL_1(-30),
        LEVEL_2(-20),
        LEVEL_3(-16),
        LEVEL_4(-12),
        LEVEL_5(-8),
        LEVEL_6(-4),
        LEVEL_7(0),
        LEVEL_8(4);

        public final int powerInDbm;

        private BroadcastingPower(int powerInDbm) {
            this.powerInDbm = powerInDbm;
        }

        public static BroadcastingPower fromDbm(int powerInDbm) {
            BroadcastingPower[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BroadcastingPower broadcastingPower = var1[var3];
                if(broadcastingPower.powerInDbm == powerInDbm) {
                    return broadcastingPower;
                }
            }

            return LEVEL_4;
        }
    }


    protected static enum BatteryLevel {
        UNKNOWN(-1.0F),
        HIGH(2.95F),
        MEDIUM(2.7F),
        LOW(0.0F);

        public final float voltage;

        private BatteryLevel(float voltage) {
            this.voltage = voltage;
        }
    }

    protected static enum FirmwareState {
        BOOTLOADER,
        APP;

        private FirmwareState() {
        }
    }
}
