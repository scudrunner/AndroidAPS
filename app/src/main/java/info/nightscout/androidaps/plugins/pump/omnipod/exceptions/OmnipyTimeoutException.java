package info.nightscout.androidaps.plugins.pump.omnipod.exceptions;

public class OmnipyTimeoutException extends Exception {
    public OmnipyTimeoutException(Exception e) { super(e); }
    public OmnipyTimeoutException() { }
}
