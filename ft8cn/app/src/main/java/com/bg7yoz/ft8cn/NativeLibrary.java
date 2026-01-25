package com.bg7yoz.ft8cn;

/**
 * Centralized native library loader to ensure the ft8cn native library
 * is loaded only once and safely. This helps prevent issues with
 * security scanners like Google Play Protect.
 * 
 * @author BG7YOZ
 */
public class NativeLibrary {
    private static final String TAG = "NativeLibrary";
    private static final String LIBRARY_NAME = "ft8cn";
    private static volatile boolean loaded = false;
    private static volatile boolean loading = false;
    private static final Object lock = new Object();

    /**
     * Load the native library in a thread-safe manner.
     * This method ensures the library is only loaded once,
     * even if called from multiple threads or classes.
     * 
     * @return true if the library was successfully loaded or already loaded
     */
    public static boolean load() {
        if (loaded) {
            return true;
        }

        synchronized (lock) {
            if (loaded) {
                return true;
            }

            if (loading) {
                // Another thread is loading, wait for it
                while (!loaded && loading) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        android.util.Log.e(TAG, "Interrupted while waiting for library load", e);
                        return false;
                    }
                }
                return loaded;
            }

            loading = true;
            try {
                System.loadLibrary(LIBRARY_NAME);
                loaded = true;
                android.util.Log.d(TAG, "Successfully loaded native library: " + LIBRARY_NAME);
                return true;
            } catch (UnsatisfiedLinkError e) {
                android.util.Log.e(TAG, "Failed to load native library: " + LIBRARY_NAME, e);
                loaded = false;
                return false;
            } catch (SecurityException e) {
                android.util.Log.e(TAG, "Security exception loading native library: " + LIBRARY_NAME, e);
                loaded = false;
                return false;
            } catch (Exception e) {
                android.util.Log.e(TAG, "Unexpected error loading native library: " + LIBRARY_NAME, e);
                loaded = false;
                return false;
            } finally {
                loading = false;
                lock.notifyAll();
            }
        }
    }

    /**
     * Check if the native library is loaded.
     * 
     * @return true if the library is loaded
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
