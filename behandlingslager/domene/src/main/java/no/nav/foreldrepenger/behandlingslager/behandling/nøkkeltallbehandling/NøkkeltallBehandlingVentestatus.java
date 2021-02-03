package no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

import java.time.LocalDate;


public class NøkkeltallBehandlingVentestatus {

    private final String behandlendeEnhet;
    private final BehandlingType behandlingType;
    private final BehandlingVenteStatus behandlingVenteStatus;
    private final LocalDate førsteUttakMåned;
    private final int antall;

    public NøkkeltallBehandlingVentestatus(String behandlendeEnhet, BehandlingType behandlingType,
                                           BehandlingVenteStatus behandlingVenteStatus, LocalDate førsteUttakMåned,
                                           int antall) {
        this.behandlendeEnhet = behandlendeEnhet;
        this.behandlingType = behandlingType;
        this.behandlingVenteStatus = behandlingVenteStatus;
        this.førsteUttakMåned = førsteUttakMåned;
        this.antall = antall;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public BehandlingVenteStatus getBehandlingVenteStatus() {
        return behandlingVenteStatus;
    }

    public LocalDate getFørsteUttakMåned() {
        return førsteUttakMåned;
    }

    public int getAntall() {
        return antall;
    }

    public String getBehandlendeEnhet() {
        return behandlendeEnhet;
    }
}
