package info.nightscout.androidaps.plugins.pump.omnipod.events;

import info.nightscout.androidaps.events.Event;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipyApiSecret;

public class EventOmnipyConfigurationComplete extends Event {

    public final String hostName;
    public final OmnipyApiSecret apiSecret;
    public final boolean isDiscovered;
    public final boolean isConnectable;
    public final boolean isAuthenticated;

    public EventOmnipyConfigurationComplete(String host,
                                            OmnipyApiSecret secret,
                                            boolean discovered,
                                            boolean connectable, boolean authenticated)
    {
        hostName = host;
        apiSecret = secret;
        isDiscovered = discovered;
        isConnectable = connectable;
        isAuthenticated = authenticated;
    }
}