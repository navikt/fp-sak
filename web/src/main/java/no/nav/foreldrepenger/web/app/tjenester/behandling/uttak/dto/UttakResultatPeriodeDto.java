package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeUtfallÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;

public class UttakResultatPeriodeDto {

    private LocalDate fom;
    private LocalDate tom;
    private List<UttakResultatPeriodeAktivitetDto> aktiviteter = new ArrayList<>();
    private PeriodeResultatType periodeResultatType;
    private String begrunnelse;
    private PeriodeResultatÅrsak periodeResultatÅrsak;
    private PeriodeUtfallÅrsak periodeUtfallÅrsak;
    private ManuellBehandlingÅrsak manuellBehandlingÅrsak;
    private GraderingAvslagÅrsak graderingAvslagÅrsak;
    private boolean flerbarnsdager;
    private boolean samtidigUttak;
    private SamtidigUttaksprosent samtidigUttaksprosent;
    private boolean graderingInnvilget;
    private UttakPeriodeType periodeType;
    private UttakUtsettelseType utsettelseType;
    private OppholdÅrsak oppholdÅrsak;
    private LocalDate mottattDato;
    private LocalDate tidligstMottattDato;

    private UttakResultatPeriodeDto() {

    }

    public PeriodeResultatÅrsak getPeriodeResultatÅrsak() {
        return periodeResultatÅrsak;
    }

    public PeriodeUtfallÅrsak getPeriodeUtfallÅrsak() {
        return periodeUtfallÅrsak;
    }

    public GraderingAvslagÅrsak getGraderingAvslagÅrsak() {
        return graderingAvslagÅrsak;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean isFlerbarnsdager() {
        return flerbarnsdager;
    }

    public boolean isSamtidigUttak() {
        return samtidigUttak;
    }

    public boolean isGraderingInnvilget() {
        return graderingInnvilget;
    }

    public ManuellBehandlingÅrsak getManuellBehandlingÅrsak() {
        return manuellBehandlingÅrsak;
    }

    public List<UttakResultatPeriodeAktivitetDto> getAktiviteter() {
        return aktiviteter;
    }

    public void leggTilAktivitet(UttakResultatPeriodeAktivitetDto aktivitetDto) {
        aktiviteter.add(aktivitetDto);
    }

    public PeriodeResultatType getPeriodeResultatType() {
        return periodeResultatType;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @JsonProperty("gradertAktivitet")
    public UttakResultatPeriodeAktivitetDto getGradertAktivitet() {
        return aktiviteter.stream().filter(UttakResultatPeriodeAktivitetDto::isGradering).findFirst().orElse(null);
    }

    public UttakPeriodeType getPeriodeType() {
        return periodeType;
    }

    public UttakUtsettelseType getUtsettelseType() {
        return utsettelseType;
    }

    public SamtidigUttaksprosent getSamtidigUttaksprosent() {
        return samtidigUttaksprosent;
    }

    public OppholdÅrsak getOppholdÅrsak() {
        return oppholdÅrsak;
    }

    @JsonProperty("periodeResultatÅrsakLovhjemmel")
    public String getPeriodeResultatÅrsakLovhjemmel() {
        if (periodeUtfallÅrsak != null) {
            return periodeResultatÅrsak.getLovHjemmelData();
        }
        return periodeResultatÅrsak == null ? null : periodeResultatÅrsak.getLovHjemmelData();
    }

    @JsonProperty("graderingsAvslagÅrsakLovhjemmel")
    public String getGraderingsAvslagÅrsakLovhjemmel() {
        return graderingAvslagÅrsak == null ? null : graderingAvslagÅrsak.getLovHjemmelData();
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public LocalDate getTidligstMottattDato() {
        return tidligstMottattDato;
    }

    public static class Builder {

        private final UttakResultatPeriodeDto kladd = new UttakResultatPeriodeDto();

        public Builder medTidsperiode(LocalDate fom, LocalDate tom) {
            kladd.fom = fom;
            kladd.tom = tom;
            return this;
        }

        public Builder medPeriodeResultatÅrsak(PeriodeResultatÅrsak årsak) {
            kladd.periodeResultatÅrsak = årsak;
            kladd.periodeUtfallÅrsak = årsak != null ? PeriodeUtfallÅrsak.fraKode(årsak.getKode()) : null;
            return this;
        }

        public Builder medPeriodeUtfallÅrsak(PeriodeUtfallÅrsak årsak) {
            kladd.periodeUtfallÅrsak = årsak;
            return this;
        }

        public Builder medGraderingAvslåttÅrsak(GraderingAvslagÅrsak graderingAvslagÅrsak) {
            kladd.graderingAvslagÅrsak = graderingAvslagÅrsak;
            return this;
        }

        public Builder medGraderingInnvilget(boolean innvilget) {
            kladd.graderingInnvilget = innvilget;
            return this;
        }

        public Builder medFlerbarnsdager(boolean flerbarnsdager) {
            kladd.flerbarnsdager = flerbarnsdager;
            return this;
        }

        public Builder medSamtidigUttak(boolean samtidigUttak) {
            kladd.samtidigUttak = samtidigUttak;
            return this;
        }

        public Builder medSamtidigUttaksprosent(SamtidigUttaksprosent samtidigUttaksprosent) {
            kladd.samtidigUttaksprosent = samtidigUttaksprosent;
            return this;
        }

        public Builder medManuellBehandlingÅrsak(ManuellBehandlingÅrsak årsak) {
            kladd.manuellBehandlingÅrsak = årsak;
            return this;
        }

        public Builder medPeriodeResultatType(PeriodeResultatType resultatType) {
            kladd.periodeResultatType = resultatType;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medUtsettelseType(UttakUtsettelseType utsettelseType) {
            kladd.utsettelseType = utsettelseType;
            return this;
        }

        public Builder medPeriodeType(UttakPeriodeType uttakPeriodeType) {
            kladd.periodeType = uttakPeriodeType;
            return this;
        }

        public Builder medOppholdÅrsak(OppholdÅrsak oppholdÅrsak) {
            kladd.oppholdÅrsak = oppholdÅrsak;
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            kladd.mottattDato = mottattDato;
            return this;
        }

        public Builder medTidligstMottattDato(LocalDate tidligstMottattDato) {
            kladd.tidligstMottattDato = tidligstMottattDato;
            return this;
        }

        public UttakResultatPeriodeDto build() {
            Objects.requireNonNull(kladd.periodeResultatType, "periodeResultatType");
            Objects.requireNonNull(kladd.periodeResultatÅrsak, "periodeResultatÅrsak");
            Objects.requireNonNull(kladd.fom, "fom");
            Objects.requireNonNull(kladd.tom, "tom");
            return kladd;
        }
    }
}
