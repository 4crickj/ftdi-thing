package eu.crick.ilmato;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.ftdi.j2xx.D2xxManager.D2xxException;

/*
 * This activity acts as a serial terminal, sending and receiving text
 * from the FTDI device in UART mode.
 * 
 * Connect the yellow and orange wires together to form a loopback device.
 * 
 * To cross over:
 * 
 * Yellow -> Orange
 * Green -> Brown
 * Purple -> Grey
 */
public class SerialActivity extends Activity {
	
	D2xxManager ftdi_manager;
	FT_Device ftdi_device;
	EditText term;
	Thread reader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_serial);
		
		
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		
		refreshDevice();
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
		
		if(reader != null)
			reader.interrupt();
		
		if(ftdi_device != null)
			ftdi_device.close();
	}
	
	private void refreshDevice()
	{
		if(reader != null)
			reader.interrupt();
		
		if(ftdi_device != null)
			ftdi_device.close();
		
		term = (EditText) findViewById(R.id.terminalText);
		
		try {	// Load the FTDI device
			ftdi_manager = D2xxManager.getInstance(this.getApplicationContext());
			int i = ftdi_manager.createDeviceInfoList(getApplicationContext());
			ftdi_device = null;
			
			if(i > 0) {
				ftdi_device = ftdi_manager.openByIndex(getApplicationContext(), 0);
			}
		} catch (D2xxException e) {
			ftdi_manager = null;
			ftdi_device = null;
		}
		
		Button send = (Button) findViewById(R.id.sendButton);
		
		if(ftdi_device == null) {
			appendTerminalLine("No devices present", true);
			send.setEnabled(false);
		} else {
			appendTerminalLine("Device loaded", true);
			
			send.setEnabled(true);
			
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
			
			int bd = Integer.parseInt(pref.getString("serial_baud", "9600"));
			ftdi_device.setBaudRate(bd);
			
			if(pref.getBoolean("serial_break", true)) {
				ftdi_device.setBreakOn();
			} else {
				ftdi_device.setBreakOff();
			}
			
			String db = pref.getString("serial_data_bits", "7");
			byte data_bits = D2xxManager.FT_DATA_BITS_8;
			
			if(db.equals("7")) {
				data_bits = D2xxManager.FT_DATA_BITS_7;
			}
			
			String sb = pref.getString("serial_stop_bits", "1");
			byte stop_bits = D2xxManager.FT_STOP_BITS_1;
			
			if(sb.equals("2")) {
				stop_bits = D2xxManager.FT_STOP_BITS_2;
			}
			
			String p = pref.getString("serial_parity", "None");
			byte parity = D2xxManager.FT_PARITY_NONE;
			
			if(p.equals("Odd")) {
				parity = D2xxManager.FT_PARITY_ODD;
			} else if(p.equals("Even")) {
				parity = D2xxManager.FT_PARITY_EVEN;
			}
			
			ftdi_device.setDataCharacteristics(data_bits, stop_bits, parity);
			
			String fc = pref.getString("serial_flow", "None");
			short flow = D2xxManager.FT_FLOW_NONE;
			
			if(fc.equals("RTS CTS")) {
				flow = D2xxManager.FT_FLOW_RTS_CTS;
			} else if(fc.equals("DTR DSR")) {
				flow = D2xxManager.FT_FLOW_DTR_DSR;
			}
			
			ftdi_device.setFlowControl(flow, (byte) 0, (byte) 0);
			
			// Continuous thread to read the Rx buffer on the device
			// and write it to screen.
			reader = new ReaderThread();
			reader.start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.serial, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			
			return true;
		}
		
		if(id == R.id.action_refresh) {
			refreshDevice();
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	/*
	 * Run when the Send button is clicked.
	 * Contains code to write text to the device.
	 */
	public void onSendClick(View v) {
		EditText line_input = (EditText) findViewById(R.id.sendText);
		final String line = line_input.getText().toString();
		
		appendTerminalLine("Sent \"" + line + "\"", true);
		line_input.setText("");
		
		Thread writer = new Thread() {
			public void run()
			{
				byte[] buf = line.getBytes();
				byte[] c = new byte[1];
				
				
				//try {
					//for(int i = 0; i < buf.length; i++) {
						//c[0] = buf[i];
						
						synchronized(ftdi_device) {
							ftdi_device.write(buf);
						}
						//Thread.sleep(20);
					//}
					//ftdi_device.purge(D2xxManager.FT_PURGE_TX);
				//} catch(InterruptedException e) { }
			}
		};
		
		writer.start();
		
	}
	
	/*
	 * Appends text to the terminal box.
	 */
	private void appendTerminalLine(String line)
	{
		appendTerminalLine(line, false);
	}
	
	/*
	 * Appends text to the terminal box. If info is false
	 * text will be prepended by and identifier indicating
	 * information text.
	 */
	private void appendTerminalLine(String line, boolean info)
	{
		if(info)
			term.append("> " + line + "\n");
		else
			term.append(line);
	}
	
	private class ReaderThread extends Thread
	{
		public void run() {
			final byte[] buf = new byte[10];
			final char[] charBuf = new char[10];
			
			for(;;) {
				try 
				{
					Thread.sleep(10);
				}
				catch (InterruptedException e) { }
				
				synchronized(ftdi_device) {
					if(ftdi_device == null) {
						return;
					}
					
					if(ftdi_device.isOpen()) {
						int t = ftdi_device.getQueueStatus();
						final int i = t > buf.length ? buf.length : t;
						
						if(i > 0) {
							ftdi_device.read(buf, i); //todo read i bytes
							ftdi_device.purge(D2xxManager.FT_PURGE_RX);
							
							for(int j = 0; j < i; j++) {
								charBuf[j] = (char) buf[j];
							}
							
							term.post(new Runnable() {	// This method is needed as Views
								public void run() {		// are not thread safe
									appendTerminalLine(String.copyValueOf(charBuf, 0, i));
									
								}
							});
						}
					}
				}
			}
		}
	}
}
