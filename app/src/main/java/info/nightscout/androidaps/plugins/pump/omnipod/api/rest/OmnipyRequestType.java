package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

public enum OmnipyRequestType {
    Ping,
    Shutdown,
    Restart,
    Token,
    CheckPassword,
    NewPod,
    SetPodParameters,
    ReadPdmAddress,
    RLInfo,
    ActivatePod,
    StartPod,
    Status,
    IsBusy,
    AckAlerts,
    DeactivatePod,
    Bolus,
    CancelBolus,
    TempBasal,
    CancelTempBasal,
    SetBasalSchedule
}
