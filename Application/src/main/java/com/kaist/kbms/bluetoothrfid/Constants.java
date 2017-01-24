package com.kaist.kbms.bluetoothrfid;

public interface Constants {

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // RFID 패킷 (SA-A100 Developer Guide ..)
    public static final byte[] READ_START = {(byte)0xBB, (byte)0x01, (byte)0x36, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x7E};
    public static final byte[] READ_STOP  = {(byte)0xBB, (byte)0x01, (byte)0x28, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x7E};
    public static final byte[] DEVICE_OFF = {(byte)0xBB, (byte)0x01, (byte)0xAB, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x7E};
    public static final byte[] READ_BLOCK = {(byte)0xBB, (byte)0x02, (byte)0x22, (byte)0x00, (byte)0x0E, (byte)0x30, (byte)0x00};
}
