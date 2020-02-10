package no.nav.foreldrepenger.domene.opptjening.dto;


import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

class FastsattOpptjeningAktivitetDto {
    private LocalDate fom;
    private LocalDate tom;
    private OpptjeningAktivitetType type;
    private OpptjeningAktivitetKlassifisering klasse;
    private String aktivitetReferanse;
    private String arbeidsgiverNavn;

    public FastsattOpptjeningAktivitetDto() {
        // trengs for deserialisering av JSON
    }

    FastsattOpptjeningAktivitetDto(LocalDate fom, LocalDate tom, OpptjeningAktivitetKlassifisering klasse) {
        this.fom = fom;
        this.tom = tom;
        this.klasse = klasse;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public OpptjeningAktivitetType getType() {
        return type;
    }

    public OpptjeningAktivitetKlassifisering getKlasse() {
        return klasse;
    }

    public String getAktivitetReferanse() {
        return aktivitetReferanse;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

}
