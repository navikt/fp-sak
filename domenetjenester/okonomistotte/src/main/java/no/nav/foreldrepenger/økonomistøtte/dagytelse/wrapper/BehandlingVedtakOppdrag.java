package no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;

public class BehandlingVedtakOppdrag {

    private final String ansvarligSaksbehandler;
    private final BehandlingResultatType behandlingResultatType;
    private final LocalDate vedtaksdato;

    public BehandlingVedtakOppdrag(String ansvarligSaksbehandler, BehandlingResultatType behandlingResultatType, LocalDate vedtaksdato) {
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
        this.vedtaksdato = vedtaksdato;
        this.behandlingResultatType = behandlingResultatType;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public BehandlingResultatType getBehandlingResultatType() {
        return behandlingResultatType;
    }

    public LocalDate getVedtaksdato() {
        return vedtaksdato;
    }
}
