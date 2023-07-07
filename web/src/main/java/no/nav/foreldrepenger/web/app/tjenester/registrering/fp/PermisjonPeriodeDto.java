package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.GraderingDto;

public class PermisjonPeriodeDto {

    @NotNull
    private LocalDate periodeFom;

    @NotNull
    private LocalDate periodeTom;

    @ValidKodeverk
    private UttakPeriodeType periodeType;

    @ValidKodeverk
    private MorsAktivitet morsAktivitet;

    private boolean harSamtidigUttak;

    @DecimalMax("100.00")
    @DecimalMin("0.00")
    @Digits(integer = 3, fraction = 2)
    @JsonDeserialize(using = GraderingDto.BigDecimalSerializer.class)
    private BigDecimal samtidigUttaksprosent;

    private boolean flerbarnsdager;

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public void setPeriodeFom(LocalDate periodeFom) {
        this.periodeFom = periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public void setPeriodeTom(LocalDate periodeTom) {
        this.periodeTom = periodeTom;
    }

    public UttakPeriodeType getPeriodeType() {
        return periodeType;
    }

    public void setPeriodeType(UttakPeriodeType periodeType) {
        this.periodeType = periodeType;
    }

    public MorsAktivitet getMorsAktivitet() {
        return morsAktivitet;
    }

    public void setMorsAktivitet(MorsAktivitet morsAktivitet) {
        this.morsAktivitet = morsAktivitet;
    }

    public boolean getHarSamtidigUttak() {
        return harSamtidigUttak;
    }

    public void setHarSamtidigUttak(boolean harSamtidigUttak) {
        this.harSamtidigUttak = harSamtidigUttak;
    }

    public BigDecimal getSamtidigUttaksprosent() {
        return samtidigUttaksprosent;
    }

    public void setSamtidigUttaksprosent(BigDecimal samtidigUttaksprosent) {
        this.samtidigUttaksprosent = samtidigUttaksprosent;
    }

    public boolean isFlerbarnsdager() {
        return flerbarnsdager;
    }

    public void setFlerbarnsdager(boolean flerbarnsdager) {
        this.flerbarnsdager = flerbarnsdager;
    }
}
