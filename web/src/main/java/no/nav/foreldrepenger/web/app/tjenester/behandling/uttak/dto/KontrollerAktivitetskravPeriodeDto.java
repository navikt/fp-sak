package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.vedtak.util.InputValideringRegex;

public class KontrollerAktivitetskravPeriodeDto {

    @NotNull
    private LocalDate fom;
    @NotNull
    private LocalDate tom;
    @NotNull
    private KontrollerAktivitetskravAvklaring avklaring;
    @NotBlank
    @NotNull
    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    private MorsAktivitet morsAktivitet;

    private boolean endret;

    public KontrollerAktivitetskravPeriodeDto() {
        //Jackson
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public KontrollerAktivitetskravAvklaring getAvklaring() {
        return avklaring;
    }

    public void setAvklaring(KontrollerAktivitetskravAvklaring avklaring) {
        this.avklaring = avklaring;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setMorsAktivitet(MorsAktivitet morsAktivitet) {
        this.morsAktivitet = morsAktivitet;
    }

    public MorsAktivitet getMorsAktivitet() {
        return morsAktivitet;
    }

    public void setEndret(boolean endret) {
        this.endret = endret;
    }

    public boolean isEndret() {
        return endret;
    }

    @Override
    public String toString() {
        return "KontrollerAktivitetskravPeriodeDto{" + "fom=" + fom + ", tom=" + tom + ", avklaring=" + avklaring
            + ", morsAktivitet=" + morsAktivitet + '}';
    }
}
