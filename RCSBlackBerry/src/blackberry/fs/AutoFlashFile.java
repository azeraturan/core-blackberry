/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry_lib 
 * File         : AutoFlashFile.java 
 * Created      : 26-mar-2010
 * *************************************************/
package com.ht.rcs.blackberry.fs;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.io.IOUtilities;

import com.ht.rcs.blackberry.utils.Check;
import com.ht.rcs.blackberry.utils.Utils;

public class AutoFlashFile {
    String filename;
    boolean hidden;
    boolean autoclose;

    private FileConnection fconn;
    private DataInputStream is;
    private OutputStream os;

    public AutoFlashFile(String filename_, boolean hidden_) {
        this.filename = filename_;
        this.hidden = hidden_;
    }

    public synchronized boolean append(byte[] message) {
        try {
            fconn = (FileConnection) Connector.open(filename,
                    Connector.READ_WRITE);
            // #ifdef DBC
            Check.asserts(fconn != null, "file fconn null");
            // #endif

            long size = fconn.fileSize();
            os = fconn.openOutputStream(size);
            // #ifdef DBC
            Check.asserts(os != null, "os null");
            // #endif

            os.write(message);

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        } finally {

            close();
        }

        return true;
    }

    public synchronized boolean append(int value) {
        byte[] repr;
        repr = Utils.intToByteArray(value);
        return append(repr);
    }

    public synchronized boolean append(String message) {
        return append(message.getBytes());
    }

    private synchronized void close() {
        try {
            if (null != is) {
                is.close();
            }

            if (null != os) {
                os.close();
            }

            if (null != fconn) {
                fconn.close();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public synchronized boolean create() {
        try {

            fconn = (FileConnection) Connector.open(filename,
                    Connector.READ_WRITE);
            // #ifdef DBC
            Check.asserts(fconn != null, "fconn null");
            // #endif

            if (fconn.exists()) {
                fconn.truncate(0);
            } else {
                fconn.create();
                os = fconn.openDataOutputStream();

            }

            fconn.setHidden(hidden);
            // #ifdef DBC
            Check.asserts(fconn.isHidden() == hidden, "Not Hidden as expected");
            // #endif
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            close();
        }

        // #ifdef DBC
        Check.ensures(exists(), "not created");
        // #endif
        return true;
    }

    public synchronized void delete() {
        try {
            fconn = (FileConnection) Connector.open(filename,
                    Connector.READ_WRITE);
            // #ifdef DBC
            Check.asserts(fconn != null, "file fconn null");
            // #endif

            if (fconn.exists()) {
                fconn.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public synchronized boolean exists() {
        try {
            fconn = (FileConnection) Connector.open(filename, Connector.READ);
            // #ifdef DBC
            Check.asserts(fconn != null, "fconn null");
            // #endif

            return fconn.exists();

        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            close();
        }
    }

    public synchronized byte[] read() {
        byte[] data = null;

        try {
            fconn = (FileConnection) Connector.open(filename, Connector.READ);
            // #ifdef DBC
            Check.asserts(fconn != null, "file fconn null");
            // #endif

            is = fconn.openDataInputStream();
            data = IOUtilities.streamToBytes(is);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            close();
        }

        return data;
    }

    public synchronized InputStream getInputStream() {
        try {
            fconn = (FileConnection) Connector.open(filename, Connector.READ);
            // #ifdef DBC
            Check.asserts(fconn != null, "file fconn null");
            // #endif

            is = fconn.openDataInputStream();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return is;
    }

    public synchronized boolean write(byte[] message) {

        try {
            fconn = (FileConnection) Connector.open(filename, Connector.WRITE);
            // #ifdef DBC
            Check.asserts(fconn != null, "file fconn null");
            // #endif

            os = fconn.openOutputStream();

            os.write(message);

        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            close();
        }

        return true;

    }

    public synchronized boolean write(int value) {
        byte[] repr = Utils.intToByteArray(value);
        return write(repr);
    }

    public boolean rename(String newFile) {
        try {
            fconn = (FileConnection) Connector.open(filename,
                    Connector.READ_WRITE);
            // #ifdef DBC
            Check.asserts(fconn != null, "file fconn null");
            // #endif

            if (fconn.exists()) {
                fconn.rename(newFile);
                filename = newFile;
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            close();
        }
        return true;
    }
}