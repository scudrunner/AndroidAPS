package info.nightscout.androidaps.plugins.pump.omnipod.events;

import info.nightscout.androidaps.events.Event;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyResult;

public class EventOmnipyApiResult extends Event {
    private final OmnipyResult _result;

    public EventOmnipyApiResult(OmnipyResult result)
    {
        _result = result;
    }

    public OmnipyResult getResult()
    {
        return _result;
    }
}
