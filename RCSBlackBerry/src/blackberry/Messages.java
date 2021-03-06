//#preprocess

/* *************************************************
 * Copyright (c) 2010 - 2012
 * HT srl,   All rights reserved.
 * 
 * Project      : RCS, RCSBlackBerry
 * *************************************************/

package blackberry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Hashtable;

import net.rim.device.api.crypto.SHA1Digest;
import net.rim.device.api.i18n.MissingResourceException;
import blackberry.config.Cfg;
import blackberry.crypto.EncryptionPKCS5;
import blackberry.debug.Check;
import blackberry.debug.Debug;
import blackberry.debug.DebugLevel;
import blackberry.interfaces.iSingleton;
import blackberry.utils.StringUtils;
import blackberry.utils.Utils;

public class Messages implements iSingleton {
    private static final long GUID = 0xd228b2db2b07ededL;

    //#ifdef DEBUG
    private static Debug debug = new Debug("Messages", DebugLevel.VERBOSE);
    //#endif

    private Hashtable hashMessages;
    private boolean initialized;

    private static Messages instance;

    private Messages() {
        init();
    }

    private static synchronized Messages getInstance() {
        if (instance == null) {
            instance = (Messages) Singleton.self().get(GUID);
            if (instance == null) {
                final Messages singleton = new Messages();
                Singleton.self().put(GUID, singleton);
                instance = singleton;
            }
        }

        return instance;
    }

    private synchronized boolean init() {
        if (initialized) {
            return true;
        }

        try {

            hashMessages = new Hashtable();

            // "/messages.bin".each{ |x| print x.ord+1," " }
            byte[] messages = new byte[] { 48, 110, 102, 116, 116, 98, 104,
                    102, 116, 47, 99, 106, 111 };
            for (int i = 0; i < messages.length; i++) {
                messages[i] = (byte) (messages[i] - 1);
            }

            InputStream stream = Messages.class.getClass().getResourceAsStream(
                    new String(messages));

            long p = 5995216111976943442L; //0x5333494a32158f52;
            String sp = Long.toString(p, 16);

            EncryptionPKCS5 encryption = new EncryptionPKCS5(produceKey("0x"
                    + sp));

            byte[] decrypted = null;
            byte[] data = Utils.inputStreamToBuffer(stream);
            try {
                
                decrypted = encryption.decryptData(data);
            } catch (Exception e) {
                //#ifdef DEBUG
                debug.error(e);
                debug.error("init, cannot decrypt, try plain");
                //#endif
                decrypted = data;
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);

            String lines = new String(decrypted);

            int posMessages = 0;

            String lastLine = "";
            while (true) {
                String currentLine = StringUtils
                        .getNextLine(lines, posMessages);
                if (currentLine == null) {
                    //#ifdef DEBUG
                    debug.trace("parseLinesConversation null line, posMessage: "
                            + posMessages);
                    //#endif
                    break;
                }
                posMessages += currentLine.length() + 1;

                String[] kv = StringUtils.splitFirst(
                        StringUtils.chop(currentLine), "=");
                //#ifdef DBC
                Check.asserts(kv.length == 2, "wrong number of tokens");
                //#endif

                if (kv.length != 2) {
                    //#ifdef DEBUG
                    debug.error("init len: " + kv.length);
                    //#endif
                    continue;
                }

                //#ifdef DBC
                Check.asserts(!hashMessages.contains(kv[0]),
                        "key already present: " + kv[0]);
                //#endif

                hashMessages.put(kv[0], kv[1]);
                //#ifdef DEBUG
                debug.trace(kv[0] + " -> " + kv[1]);
                //#endif
            }

            //#ifdef DEBUG
            debug.info("init, decoded messages: " + hashMessages.size());
            //#endif
            initialized = true;
        } catch (Exception ex) {
            //#ifdef DEBUG
            debug.error(ex);
            debug.error("init");
            //#endif

            return false;
        }
        return true;
    }

    public static String getString(String key) {
        return Messages.getInstance().getStringInstance(key);
    }

    public String getStringInstance(String key) {
        if (!initialized) {

            if (!init()) {
                return null;
            }
        }
        try {
            //#ifdef DBC
            Check.asserts(hashMessages.containsKey(key), "no key known: " + key);
            //#endif

            String str = (String) hashMessages.get(key);
            return str;
        } catch (MissingResourceException e) {
            //#ifdef DEBUG
            debug.error(e);
            debug.error("getString");
            //#endif

            return '!' + key + '!';
        }
    }

    /**
     * Reads the contents of the key file and converts this into a
     * <code>Key</code>.
     * 
     * @return The <code>Key</code> object.
     * @throws BuildException
     *             If the contents of the key file cannot be read.
     */
    public static byte[] produceKey(String key) {

        try {
            //#ifdef DEBUG
            debug.trace("produceKey key: " + key + " " + key.length());
            //#endif

            String salt = Cfg.RANDOM;

            final SHA1Digest digest = new SHA1Digest();

            for (int i = 0; i < 128; i++) {
                digest.update(salt.getBytes());
                digest.update(key.getBytes());
                digest.update(digest.getDigest());
            }

            byte[] sha1 = digest.getDigest();

            byte[] aes_key = new byte[16];
            System.arraycopy(sha1, 0, aes_key, 0, aes_key.length);

            //#ifdef DEBUG
            debug.trace("produceKey: " + Utils.byteArrayToHex(aes_key));
            //#endif
            return aes_key;
        } catch (Exception e) {
            //#ifdef DEBUG
            debug.error(e);
            debug.error("produceKey");
            //#endif

            return null;
        }

    }

}
