package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class UtsettelseDto {

    @NotNull
    private LocalDate periodeFom;

    @NotNull
    private LocalDate periodeTom;

    @ValidKodeverk
    private UttakPeriodeType periodeForUtsettelse;

    @ValidKodeverk
    private UtsettelseÅrsak arsakForUtsettelse;

    @ValidKodeverk
    private MorsAktivitet morsAktivitet;

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public void setPeriodeFom(LocalDate periodeFom) {
        this.periodeFom = periodeFom;
    }

    public void setPeriodeTom(LocalDate periodeTom) {
        this.periodeTom = periodeTom;
    }

    public UttakPeriodeType getPeriodeForUtsettelse() {
        return periodeForUtsettelse;
    }

    public void setPeriodeForUtsettelse(UttakPeriodeType periodeForUtsettelse) {
        this.periodeForUtsettelse = periodeForUtsettelse;
    }

    public void setArsakForUtsettelse(UtsettelseÅrsak arsakForUtsettelse) {
        this.arsakForUtsettelse = arsakForUtsettelse;
    }

    public UtsettelseÅrsak getArsakForUtsettelse() {
        return arsakForUtsettelse;
    }

    public MorsAktivitet getMorsAktivitet() {
        return morsAktivitet;
    }

    public void setMorsAktivitet(MorsAktivitet morsAktivitet) {
        this.morsAktivitet = morsAktivitet;
    }

}
