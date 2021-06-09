package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;

public class ØkonomiKvittering {

    private Long behandlingId;
    private Long fagsystemId;
    private String meldingKode;
    private String beskrMelding;
    private Alvorlighetsgrad alvorlighetsgrad;


    public Long getBehandlingId() {
        return behandlingId;
    }

    public Long getFagsystemId() {
        return fagsystemId;
    }

    public Alvorlighetsgrad getAlvorlighetsgrad() {
        return alvorlighetsgrad;
    }

    public String getMeldingKode() {
        return meldingKode;
    }

    public String getBeskrMelding() {
        return beskrMelding;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setFagsystemId(Long fagsystemId) {
        this.fagsystemId = fagsystemId;
    }

    public void setMeldingKode(String meldingKode) {
        this.meldingKode = meldingKode;
    }

    public void setAlvorlighetsgrad(Alvorlighetsgrad alvorlighetsgrad) {
        this.alvorlighetsgrad = alvorlighetsgrad;
    }

    public void setBeskrMelding(String beskrMelding) {
        this.beskrMelding = beskrMelding;
    }
}
