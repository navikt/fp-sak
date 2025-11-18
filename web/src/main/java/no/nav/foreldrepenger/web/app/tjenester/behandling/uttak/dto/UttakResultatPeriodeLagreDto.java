package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class UttakResultatPeriodeLagreDto {

    @NotNull
    private LocalDate fom;

    @NotNull
    private LocalDate tom;

    @NotNull
    @Size(max = 100) //Ingen aktivitet for oppholdsperiode
    private List<@Valid UttakResultatPeriodeAktivitetLagreDto> aktiviteter;

    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @Size(max = 4000)
    private String begrunnelse;

    @NotNull
    @ValidKodeverk
    private PeriodeResultatType periodeResultatType;

    @NotNull
    @ValidKodeverk
    private PeriodeResultatÅrsak periodeResultatÅrsak;

    @NotNull
    @ValidKodeverk
    private UttakUtsettelseType utsettelseType;

    @NotNull
    @ValidKodeverk
    private OppholdÅrsak oppholdÅrsak;

    @NotNull
    private boolean flerbarnsdager;

    private boolean samtidigUttak;

    @Valid
    private SamtidigUttaksprosent samtidigUttaksprosent;

    private boolean graderingInnvilget;

    @NotNull
    @ValidKodeverk
    private GraderingAvslagÅrsak graderingAvslagÅrsak;

    private LocalDate mottattDato;

    UttakResultatPeriodeLagreDto() {
        //for jackson
    }

    public List<UttakResultatPeriodeAktivitetLagreDto> getAktiviteter() {
        return aktiviteter;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public PeriodeResultatType getPeriodeResultatType() {
        return periodeResultatType;
    }

    public GraderingAvslagÅrsak getGraderingAvslagÅrsak() {
        if (graderingAvslagÅrsak == null || graderingInnvilget) {
            return GraderingAvslagÅrsak.UKJENT;
        }
        return graderingAvslagÅrsak;
    }

    public PeriodeResultatÅrsak getPeriodeResultatÅrsak() {
        return periodeResultatÅrsak != null ? periodeResultatÅrsak : PeriodeResultatÅrsak.UKJENT;
    }

    public OppholdÅrsak getOppholdÅrsak() {
        return oppholdÅrsak == null ? OppholdÅrsak.UDEFINERT : oppholdÅrsak;
    }

    public boolean isFlerbarnsdager() {
        return flerbarnsdager;
    }

    public boolean isSamtidigUttak() {
        return samtidigUttak;
    }

    public SamtidigUttaksprosent getSamtidigUttaksprosent() {
        if (isSamtidigUttak()) {
            return samtidigUttaksprosent;
        }
        return null;
    }

    public boolean isGraderingInnvilget() {
        return graderingInnvilget;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public UttakUtsettelseType getUtsettelseType() {
        return utsettelseType == null ? UttakUtsettelseType.UDEFINERT : utsettelseType;
    }

    public static class Builder {

        private final UttakResultatPeriodeLagreDto kladd = new UttakResultatPeriodeLagreDto();

        public Builder() {
            kladd.aktiviteter = Collections.emptyList();
        }

        public Builder medTidsperiode(LocalDate fom, LocalDate tom) {
            kladd.fom = fom;
            kladd.tom = tom;
            return this;
        }

        public Builder medAktiviteter(List<UttakResultatPeriodeAktivitetLagreDto> aktiviteter) {
            kladd.aktiviteter = aktiviteter;
            return this;
        }

        public Builder medPeriodeResultatType(PeriodeResultatType type) {
            kladd.periodeResultatType = type;
            return this;
        }

        public Builder medPeriodeResultatÅrsak(PeriodeResultatÅrsak årsak) {
            kladd.periodeResultatÅrsak = årsak;
            return this;
        }

        public Builder medOppholdÅrsak(OppholdÅrsak oppholdÅrsak) {
            kladd.oppholdÅrsak = oppholdÅrsak;
            return this;
        }

        public Builder medGraderingAvslåttÅrsak(GraderingAvslagÅrsak graderingAvslagÅrsak) {
            kladd.graderingAvslagÅrsak = graderingAvslagÅrsak;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medSamtidigUttak(boolean samtidigUttak) {
            kladd.samtidigUttak = samtidigUttak;
            return this;
        }

        public Builder medSamtidigUttaksprosent (SamtidigUttaksprosent samtidigUttaksprosent) {
            kladd.samtidigUttaksprosent = samtidigUttaksprosent;
            return this;
        }

        public Builder medFlerbarnsdager(boolean flerbarnsdager) {
            kladd.flerbarnsdager = flerbarnsdager;
            return this;
        }

        public Builder medGraderingInnvilget(boolean innvilget) {
            kladd.graderingInnvilget = innvilget;
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            kladd.mottattDato = mottattDato;
            return this;
        }

        public Builder medUtsettelseType(UttakUtsettelseType utsettelseType) {
            kladd.utsettelseType = utsettelseType;
            return this;
        }

        public UttakResultatPeriodeLagreDto build() {
            Objects.requireNonNull(kladd.fom);
            Objects.requireNonNull(kladd.tom);
            return kladd;
        }
    }
}
