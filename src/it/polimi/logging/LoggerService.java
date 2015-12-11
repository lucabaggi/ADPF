/*
 * Copyright 2015 Luca Baggi, Marco Mezzanotte
 * 
 * This file is part of ADPF.
 *
 *  ADPF is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ADPF is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with ADPF.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.polimi.logging;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class LoggerService extends IntentService{
	private static String TAG = "LoggerService";

	public enum LogMod {
		silent,
		active
	}
	private static LogMod mode = LogMod.silent;

	public static final String ACTION_LOG = "LOGGER_SERVICE_ACTION_LOG";
	public static final String ACTION_SET_MODE = "LOGGER_SERVICE_ACTION_SET_MODE";
	public static final String ACTION_CLEANUP = "LOGGER_SERVICE_ACTION_CLEANUP"; // Set by an alarm for daily old log files cleanup

	public static final String EXTRA_LOG = "EXTRA_LOG";
	public static final String EXTRA_MODE = "EXTRA_MODE";

	//Indirizzo server
	private static final String LOGSTASH_SERVER_URL = "http://192.168.43.170";

	//porta UDP del server in ascolto
	private static final int LOGSTASH_UDP_JSON_PORT = 9876;
	private static final String LOGSTASH_FILE_PREFIX= "logstash_";
	private static final int MAX_LOG_DAYS = 7;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private static final int DAY = 24*60*60*1000; // in milliseconds

	/**
	 * Variabile che indica il drift tra l'orario calcolato dal server NTP e l'orario settato sul device locale
	 */
	public static long NTP_DELAY = 0;

	public LoggerService() {
		super("LoggerService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		setCleanupWakeAlarm(DAY);
	}

	/**
	 * Start this service to perform a writing action with the given parameters. If
	 * the service is already performing a task this action will be queued.*
	 *
	 * @param context
	 * @param log the log row to be written
	 */
	public static void writeToFile(Context context, String log) {
		Intent intent = new Intent(context, LoggerService.class);
		intent.setAction(ACTION_LOG);
		intent.putExtra(EXTRA_LOG, log);

		context.startService(intent);
	}

	/**
	 * Start this service to change the way the service behaves. If
	 * the service is already performing a task this action will be queued.
	 *
	 * @param context
	 * @param newMode the new mode ordinal to be set
	 */
	public static void changeMode(final Context context, final LogMod newMode) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				if(newMode.equals(LogMod.silent)){
					SntpClient client = new SntpClient();
					Log.d(TAG, "Sync con NTP in corso...");
					//faccio il sync con il server NTP
					if(client.requestTime("192.168.43.160", 5000)){
						long deviceTime = Calendar.getInstance().getTimeInMillis();
						NTP_DELAY = (client.getNtpTime() - deviceTime);
						Log.i(TAG, "Sincronizzazione avvenuta. TimeDiff: " + NTP_DELAY);
					}
					Log.i(TAG, "Sync end.");
				}


				Intent intent = new Intent(context, LoggerService.class);
				intent.setAction(ACTION_SET_MODE);
				intent.putExtra(EXTRA_MODE, newMode.ordinal());

				context.startService(intent);
			}
		}).start();

	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent == null) return;
		String action = intent.getAction();

		if (action != null) {
			if (action.equalsIgnoreCase(ACTION_LOG)) {
				String log = intent.getStringExtra(EXTRA_LOG);
				if (TextUtils.isEmpty(log)) return;
				Log.d(TAG, "mode:"+this.mode+". got log:"+log);

				switch(this.mode){
				case silent:
					writeLogToFile(log);
					break;
				case active:
					sendLogToServer(null, log);
					break;
				default:
					break;
				}
			} else if (action.equalsIgnoreCase(ACTION_SET_MODE)) {
				int newMode = intent.getIntExtra(EXTRA_MODE, LogMod.silent.ordinal());
				setLogMode(LogMod.values()[newMode]);
			} else if (action.equalsIgnoreCase(ACTION_CLEANUP)) {
				// delete old log file if needed. only keep 7 days of logs
				deleteOldLogFile();
				String dateStr = dateFormat.format(new Date());
				String fileName = LOGSTASH_FILE_PREFIX + dateStr;
				deleteFile(fileName);
			}
		}
	}


	private void sendLogToServer(Socket socket, String logStr) {
		if (logStr == null || socket == null) return;
		//Socket socket;

		if (socket == null) return;
		int msg_length = logStr.length();
		byte []message = logStr.getBytes();
		//if (host != null) {
		//DatagramPacket p = new DatagramPacket(message, msg_length, host, LOGSTASH_UDP_JSON_PORT);
		try {
			socket.getOutputStream().write(message);

			//socket.send(p);
		} catch (IOException e) {
			Log.d(TAG, "couldn't send:"+e.toString());
			return;
		}
		// }


	}

	private void writeLogToFile(String log) {
		String dateStr = dateFormat.format(new Date());
		String fileName = LOGSTASH_FILE_PREFIX + dateStr;
		BufferedWriter bw = null;
		try {
			FileOutputStream outputStream = openFileOutput(fileName, Context.MODE_APPEND);
			DataOutputStream out = new DataOutputStream(outputStream);
			bw = new BufferedWriter(new OutputStreamWriter(out));
			bw.write(log);
			bw.newLine();
		} catch (FileNotFoundException e) {
			Log.d(TAG, "couldn't write log:"+e.toString());
		} catch (IOException e) {
			Log.d(TAG, "couldn't write log:"+e.toString());
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					Log.d(TAG, "failed to close BufferedWriter:"+e.toString());
				}
			}
		}
	}

	private void setLogMode(LogMod newMode) {
		if (newMode == this.mode) return;
		LogMod oldMode = this.mode;
		this.mode = newMode;
		if (oldMode == LogMod.silent && newMode == LogMod.active) {
			// activating the logging, send all the accumulated logs
			flushLogsToServer();
		}
	}

	private void deleteOldLogFile() {
		// get the date of MAX_LOG_DAYS days ago
		String dateStr = getDayString(-MAX_LOG_DAYS);

		// delete the old (week ago) file
		String fileName = LOGSTASH_FILE_PREFIX + dateStr;
		Log.e("LoggerService", "delete file " + fileName);

		deleteFile(fileName);

		// schedule the logs deletion to occur once a day
		setCleanupWakeAlarm(DAY);
	}

	private String getDayString(int offset) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_YEAR, offset);
		Date newDate = calendar.getTime();
		String dateStr = dateFormat.format(newDate);
		return dateStr;
	}

	private void flushLogsToServer() {
		// send log file one by one (each log file is a day of logs)
		for (int i=MAX_LOG_DAYS; i >= 0; i--) {
			String dateStr = getDayString(-i);
			String fileName = LOGSTASH_FILE_PREFIX + dateStr;
			sendLogFile(fileName);
			// delete the log file
			//deleteFile(fileName);
		}
	}

	/**
	 * Sends a log file to the server, line by line - each line is a separate log.
	 * @param fileName log file name
	 */
	private void sendLogFile(String fileName) {
		FileInputStream fstream = null;
		try {
			fstream = openFileInput(fileName);
		} catch (FileNotFoundException e) {
			Log.d(TAG, "couldn't open log file"+e.toString());
			return;
		}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		Socket socket = null;
		InetAddress host;
		try {
			String log = "";


			host = InetAddress.getByName(new URL(LOGSTASH_SERVER_URL).getHost());
			socket  = new Socket(host, 9876);

			while ((log = br.readLine()) != null) {
				sendLogToServer(socket,log+"\n");
			}


		} catch (IOException e) {
			Log.d(TAG, "couldn't send log to server:"+e.toString());
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if(socket != null)return;
			} catch (IOException e) {
				Log.d(TAG, "Failed to close BufferedReader:"+e.toString());
			}
		}
	}

	private void setCleanupWakeAlarm(long interval) {
		AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval,
				PendingIntent.getBroadcast(this, 0, new Intent(ACTION_CLEANUP), 0));
	}
}
