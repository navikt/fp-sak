package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeUtfallÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class UttakResultatPeriodeLagreDto {

    private static final Logger LOG = LoggerFactory.getLogger(UttakResultatPeriodeLagreDto.class);

    @NotNull
    private LocalDate fom;

    @NotNull
    private LocalDate tom;

    @Valid
    @NotNull
    @Size(max = 100) //Ingen aktivitet for oppholdsperiode
    private List<UttakResultatPeriodeAktivitetLagreDto> aktiviteter;

    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @Size(max = 4000)
    private String begrunnelse;

    @NotNull
    @ValidKodeverk
    private PeriodeResultatType periodeResultatType;

    @ValidKodeverk
    private PeriodeResultatÅrsak periodeResultatÅrsak;

    @ValidKodeverk
    private PeriodeUtfallÅrsak periodeUtfallÅrsak;

    @NotNull
    @ValidKodeverk
    private OppholdÅrsak oppholdÅrsak;

    private boolean flerbarnsdager;

    private boolean samtidigUttak;

    @Valid
    private SamtidigUttaksprosent samtidigUttaksprosent;

    private boolean graderingInnvilget;

    @NotNull
    @ValidKodeverk
    private GraderingAvslagÅrsak graderingAvslagÅrsak;

    private LocalDate mottattDato;

    UttakResultatPeriodeLagreDto() { //NOSONAR
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
        return periodeResultatÅrsak == null ? PeriodeResultatÅrsak.UKJENT : periodeResultatÅrsak;
    }

    public PeriodeUtfallÅrsak getPeriodeUtfallÅrsak() {
        return periodeUtfallÅrsak == null ? PeriodeUtfallÅrsak.UKJENT : periodeUtfallÅrsak;
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

    public static PeriodeResultatÅrsak utledPerioderesultatÅrsak(UttakResultatPeriodeLagreDto dto) {
        if (dto.getPeriodeResultatÅrsak() == null && dto.periodeUtfallÅrsak == null) {
            LOG.info("UTTAK UTFALL begge null");
        } else if (dto.getPeriodeResultatÅrsak() == null) {
            LOG.info("UTTAK UTFALL ULIKT periode null utfall {}", dto.getPeriodeUtfallÅrsak().getKode());
        } else if (dto.getPeriodeUtfallÅrsak() == null) {
            LOG.info("UTTAK UTFALL ULIKT periode {} utfall null", dto.getPeriodeResultatÅrsak().getKode());
        } else if (!dto.getPeriodeUtfallÅrsak().getKode().equals(dto.getPeriodeResultatÅrsak().getKode())) {
            LOG.info("UTTAK UTFALL ULIKT periode {} utfall {}", dto.getPeriodeResultatÅrsak().getKode(), dto.getPeriodeUtfallÅrsak().getKode());
        } else {
            LOG.info("UTTAK UTFALL lik kode");
        }
        // Vent med denne. Frontend sender ned getPeriodeUtfallÅrsak = den som ble sendt opp - uten at den røres i koden.
        // Når frontend oppdaterer  begge felt ta den tilbake
        /*
        if (dto.getPeriodeUtfallÅrsak() != null && !PeriodeUtfallÅrsak.UKJENT.equals(dto.getPeriodeUtfallÅrsak())) {
            if (InnvilgetÅrsak.kodeMap().containsKey(dto.getPeriodeUtfallÅrsak().getKode())) {
                return InnvilgetÅrsak.kodeMap().get(dto.getPeriodeUtfallÅrsak().getKode());
            } else if (IkkeOppfyltÅrsak.kodeMap().containsKey(dto.getPeriodeUtfallÅrsak().getKode())) {
                return IkkeOppfyltÅrsak.kodeMap().get(dto.getPeriodeUtfallÅrsak().getKode());
            } else {
                throw new IllegalArgumentException("Utviklerfeil - finner ikke koden fra periodeUtfallÅrsak");
            }
        }
        */
        return dto.getPeriodeResultatÅrsak();
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
            kladd.periodeUtfallÅrsak = årsak != null ? PeriodeUtfallÅrsak.fraKode(årsak.getKode()) : null;
            return this;
        }

        public Builder medPeriodeUtfallÅrsak(PeriodeUtfallÅrsak årsak) {
            kladd.periodeUtfallÅrsak = årsak;
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

        public UttakResultatPeriodeLagreDto build() {
            Objects.requireNonNull(kladd.fom);
            Objects.requireNonNull(kladd.tom);
            return kladd;
        }
    }
}
