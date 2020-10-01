package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka;

import java.time.LocalDate;

import no.nav.vedtak.konfig.Tid;

public class Opplysningsperiode {
    private LocalDate fraOgMed;

    private LocalDate tilOgMed;

    public Opplysningsperiode() {
        fraOgMed = null;
        tilOgMed = Tid.TIDENES_ENDE;
    }

    public Opplysningsperiode(LocalDate fraOgMed, LocalDate tilOgMed) {
        this.fraOgMed = fraOgMed;
        if (tilOgMed != null) {
            this.tilOgMed = tilOgMed;
        } else {
            this.tilOgMed = Tid.TIDENES_ENDE;
        }
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }
}
