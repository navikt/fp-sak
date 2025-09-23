package no.nav.foreldrepenger.web.app.tjenester.datavarehus;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

public class GenererVedtaksXmlDvhDto implements AbacDto {

    @NotNull
    private LocalDateTime fom;

    @NotNull
    private LocalDateTime tom;

    @Pattern(regexp = InputValideringRegex.KODEVERK)
    @Size(min = 1, max = 50)
    @NotNull
    private String fagsakYtelseType;

    public GenererVedtaksXmlDvhDto() {
    }

    public GenererVedtaksXmlDvhDto(LocalDateTime fomTidspunkt, LocalDateTime tomTidspunkt, String fagsakYtelseType) {
        this.fom = fomTidspunkt;
        this.tom = tomTidspunkt;
        this.fagsakYtelseType = fagsakYtelseType;
    }

    public LocalDateTime getFom() {
        return fom;
    }

    public void setFom(LocalDateTime fom) {
        this.fom = fom;
    }

    public LocalDateTime getTom() {
        return tom;
    }

    public void setTom(LocalDateTime tom) {
        this.tom = tom;
    }

    public String getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public void setFagsakYtelseType(String fagsakYtelseType) {
        this.fagsakYtelseType = fagsakYtelseType;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett();
    }
}
