package eu.crick.ilmato;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
 */
public class SerialActivity extends Activity {
	
	D2xxManager ftdi_manager;
	FT_Device ftdi_device;
	EditText term;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_serial);
		
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
		
		if(ftdi_device == null) {
			appendTerminalLine("No devices present", true);
			Button send = (Button) findViewById(R.id.sendButton);
			send.setEnabled(false);
		} else {
			appendTerminalLine("Device loaded", true);
			
			// Continuous thread to read the Rx buffer on the device
			// and write it to screen.
			new Thread(new Runnable() {
				public void run() {
					final byte[] buf = new byte[100];
					
					for(;;) {
						try 
						{
							Thread.sleep(10);
						}
						catch (InterruptedException e) { }
						
						if(ftdi_device.isOpen()) {
							int i = ftdi_device.getQueueStatus();
							
							if(i > 0) {
								if (i > buf.length){
									i = buf.length;
								}
								
								ftdi_device.read(buf, i, 100); //todo read i bytes
								ftdi_device.purge(D2xxManager.FT_PURGE_RX);
								
								term.post(new Runnable() {	// This method is needed as Views
									public void run() {		// are not thread safe
										appendTerminalLine(new String(buf));
									}
								});
							}
						}
					}
				}
			}).start();
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
		return super.onOptionsItemSelected(item);
	}
	
	/*
	 * Run when the Send button is clicked.
	 * Contains code to write text to the device.
	 */
	public void onSendClick(View v) {
		EditText line_input = (EditText) findViewById(R.id.sendText);
		String line = line_input.getText().toString();
		
		appendTerminalLine("Sent \"" + line + "\"", true);
		line_input.setText("");
		
		ftdi_device.write(line.getBytes());
	}
	
	/*
	 * Appends text to the terminal box.
	 */
	private void appendTerminalLine(String line) {
		appendTerminalLine(line, false);
	}
	
	/*
	 * Appends text to the terminal box. If info is false
	 * text will be prepended by and identifier indicating
	 * information text.
	 */
	private void appendTerminalLine(String line, boolean info) {
		if(info)
			term.append("> " + line + "\n");
		else
			term.append(line);
	}
}
