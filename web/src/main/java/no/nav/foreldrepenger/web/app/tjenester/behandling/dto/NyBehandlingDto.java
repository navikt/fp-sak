package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class NyBehandlingDto {
    @NotNull
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @ValidKodeverk
    private BehandlingType behandlingType;

    @ValidKodeverk
    private BehandlingÅrsakType behandlingArsakType;

    @Valid
    private boolean nyBehandlingEtterKlage;

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setBehandlingType(BehandlingType behandlingType) {
		this.behandlingType = behandlingType;
	}

    public BehandlingType getBehandlingType() {
		return behandlingType;
	}

    public boolean getNyBehandlingEtterKlage() {
        return nyBehandlingEtterKlage;
    }

    public void setBehandlingArsakType(BehandlingÅrsakType behandlingArsakType) {
		this.behandlingArsakType = behandlingArsakType;
	}

    public BehandlingÅrsakType getBehandlingArsakType() {
		return behandlingArsakType;
	}

}
