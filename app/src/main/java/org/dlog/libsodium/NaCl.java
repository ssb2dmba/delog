package org.dlog.sodium;


import java.util.logging.Level;
import java.util.logging.Logger;

public class NaCl {
    private static final Logger LOGGER= Logger.getLogger(NaCl.class.getName());

    static {
        String librarypath=System.getProperty("java.library.path");
        LOGGER.log(Level.INFO,"librarypath="+librarypath);
        System.loadLibrary("sodiumjni");
    }

    public static Sodium sodium() {
        Sodium.sodium_init();
        return SingletonHolder.SODIUM_INSTANCE;
    }

    private static final class SingletonHolder {
        public static final Sodium SODIUM_INSTANCE = new Sodium();
    }

    private NaCl() {
    }
}