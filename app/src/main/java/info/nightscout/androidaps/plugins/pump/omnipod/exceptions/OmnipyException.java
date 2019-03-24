package info.nightscout.androidaps.plugins.pump.omnipod.exceptions;

public class OmnipyException extends Exception {
    public Exception innerException;

    public OmnipyException() {}
    public OmnipyException(Exception e)
    {
        innerException = e;
    }
}
