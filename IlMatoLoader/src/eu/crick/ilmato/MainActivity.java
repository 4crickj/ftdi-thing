package eu.crick.ilmato;

import android.app.Activity;
import android.os.Bundle;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.content.Intent;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.D2xxManager.D2xxException;

public class MainActivity extends Activity {
	
	D2xxManager ftdi_manager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		try {
			ftdi_manager = D2xxManager.getInstance(this.getApplicationContext());
		} catch (D2xxException e) {
			ftdi_manager = null;
		}
		
		updateFTDIStatus();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onStatusButtonClick(View v) {
		updateFTDIStatus();
	}
	
	public void onSerialClick(View v) {
		Intent intent = new Intent(this, SerialActivity.class);
		startActivity(intent);
	}
	
	
	private void updateFTDIStatus() {
		// Checks if FTDI programmer is plugged in and updates
		// the status text
		
		Button btn = (Button) findViewById(R.id.button_status);
		GridLayout ldr = (GridLayout) findViewById(R.id.layout_loader);
		
		if(ftdi_manager == null) {
			btn.setText("Failed to load FTDI driver");
			btn.setBackgroundColor(getResources().getColor(R.color.status_offline));
			ldr.setVisibility(View.INVISIBLE);
			return;
		}
		
		int devices = 0;
		
		devices = ftdi_manager.createDeviceInfoList(getApplicationContext());
		
		if(devices > 0) {
			btn.setText("Connected");
			btn.setBackgroundColor(getResources().getColor(R.color.status_online));
			ldr.setVisibility(View.VISIBLE);
		} else {
			btn.setText("Not connected");
			btn.setBackgroundColor(getResources().getColor(R.color.status_offline));
			ldr.setVisibility(View.INVISIBLE);
		}
	}
}
