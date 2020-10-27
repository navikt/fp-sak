package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType.PERIODE_OK;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType.PERIODE_OK_ENDRET;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.KontrollerFaktaPeriode;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class KontrollerFaktaPeriodeLagreDto {

    @NotNull
    private LocalDate tom;

    @NotNull
    private LocalDate fom;

    @ValidKodeverk
    private UttakPeriodeType uttakPeriodeType = UttakPeriodeType.UDEFINERT;

    @ValidKodeverk
    private UtsettelseÅrsak utsettelseÅrsak = UtsettelseÅrsak.UDEFINERT;

    @ValidKodeverk
    private OverføringÅrsak overføringÅrsak = OverføringÅrsak.UDEFINERT;

    @ValidKodeverk
    private OppholdÅrsak oppholdÅrsak = OppholdÅrsak.UDEFINERT;

    @ValidKodeverk
    private UttakPeriodeVurderingType resultat = UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT;

    @Valid
    @Size(max = 10)
    private List<UttakDokumentasjonDto> dokumentertePerioder;

    @Min(0)
    @Max(100)
    @Digits(integer = 3, fraction = 2)
    private BigDecimal arbeidstidsprosent;

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    @Valid
    private ArbeidsgiverLagreDto arbeidsgiver;

    private boolean erArbeidstaker;
    private boolean erFrilanser;
    private boolean erSelvstendig;
    private boolean samtidigUttak;

    @Valid
    private SamtidigUttaksprosent samtidigUttaksprosent;

    private boolean flerbarnsdager;

    @ValidKodeverk
    private MorsAktivitet morsAktivitet = MorsAktivitet.UDEFINERT;

    @ValidKodeverk
    private FordelingPeriodeKilde periodeKilde = FordelingPeriodeKilde.SØKNAD;

    private LocalDate mottattDato;


    KontrollerFaktaPeriodeLagreDto() {//NOSONAR
        //for jackson
    }

    public LocalDate getTom() {
        return tom;
    }

    public LocalDate getFom() {
        return fom;
    }

    public UttakPeriodeType getUttakPeriodeType() {
        return uttakPeriodeType;
    }

    public Årsak getUtsettelseÅrsak() {
        return utsettelseÅrsak;
    }

    public OverføringÅrsak getOverføringÅrsak() {
        return overføringÅrsak;
    }

    public OppholdÅrsak getOppholdÅrsak() {
        return oppholdÅrsak;
    }

    public BigDecimal getArbeidstidsprosent() {
        return arbeidstidsprosent;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public List<UttakDokumentasjonDto> getDokumentertePerioder() {
        return dokumentertePerioder == null ? Collections.emptyList() : dokumentertePerioder;
    }

    public UttakPeriodeVurderingType getResultat() {
        return resultat;
    }

    public boolean erAvklartDokumentert() {
        return PERIODE_OK.equals(resultat) || PERIODE_OK_ENDRET.equals(resultat);
    }

    public ArbeidsgiverLagreDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public boolean getErArbeidstaker() {
        return erArbeidstaker;
    }

    public boolean getErFrilanser() {
        return erFrilanser;
    }

    public boolean getErSelvstendig() {
        return erSelvstendig;
    }

    public boolean getSamtidigUttak() {
        return samtidigUttak;
    }

    public SamtidigUttaksprosent getSamtidigUttaksprosent() {
        return samtidigUttaksprosent;
    }

    public MorsAktivitet getMorsAktivitet() {
        return morsAktivitet;
    }

    public boolean isFlerbarnsdager() {
        return flerbarnsdager;
    }

    public FordelingPeriodeKilde getPeriodeKilde() {
        return periodeKilde;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    @JsonIgnore
    public Optional<Årsak> getÅrsak() {
        if (!Objects.equals(UtsettelseÅrsak.UDEFINERT, utsettelseÅrsak)) {
            return Optional.of(utsettelseÅrsak);
        }
        if (!Objects.equals(OverføringÅrsak.UDEFINERT, overføringÅrsak)) {
            return Optional.of(overføringÅrsak);
        }
        return Optional.empty();
    }

    public static class Builder {

        private KontrollerFaktaPeriodeLagreDto kladd = new KontrollerFaktaPeriodeLagreDto();

        public Builder() {
        }

        public Builder(KontrollerFaktaPeriode periode, ArbeidsgiverLagreDto arbeidsgiver) {
            medPeriode(periode.getOppgittPeriode().getFom(), periode.getOppgittPeriode().getTom());
            medUttakPeriodeType(periode.getOppgittPeriode().getPeriodeType());
            medArbeidstidsprosent(periode.getOppgittPeriode().getArbeidsprosent());
            medBegrunnelse(periode.getOppgittPeriode().getBegrunnelse().orElse(null));
            medUttakPeriodeVurderingType(periode.getVurdering());
            medArbeidsgiver(arbeidsgiver);
            medArbeidstaker(periode.getOppgittPeriode().getErArbeidstaker());
            medFrilans(periode.getOppgittPeriode().getErFrilanser());
            medSelvstendig(periode.getOppgittPeriode().getErSelvstendig());
            medSamtidigUttak(periode.getOppgittPeriode().isSamtidigUttak());
            medSamtidigUttaksprosent(periode.getOppgittPeriode().getSamtidigUttaksprosent());
            medMorsaktivitet(periode.getOppgittPeriode().getMorsAktivitet());
            medFlerbarnsdager(periode.getOppgittPeriode().isFlerbarnsdager());
            medPeriodeKilde(periode.getOppgittPeriode().getPeriodeKilde());
            medMottattDato(periode.getOppgittPeriode().getMottattDato());

            if (periode.getOppgittPeriode().erUtsettelse()) {
                medUtsettelseÅrsak((UtsettelseÅrsak) periode.getOppgittPeriode().getÅrsak()); //NOSONAR
            } else if (periode.getOppgittPeriode().erOverføring()) {
                medOverføringÅrsak((OverføringÅrsak) periode.getOppgittPeriode().getÅrsak()); //NOSONAR
            } else if (periode.getOppgittPeriode().erOpphold()) {
                medOppholdÅrsak((OppholdÅrsak) periode.getOppgittPeriode().getÅrsak()); //NOSONAR
            }

            medUttakDokumentasjon(periode.getDokumentertePerioder().stream()
                .map(UttakDokumentasjonDto::new)
                .collect(Collectors.toList()));
        }

        public Builder medPeriode(LocalDate fom, LocalDate tom) {
            kladd.fom = fom;
            kladd.tom = tom;
            return this;
        }

        public Builder medUttakPeriodeType(UttakPeriodeType uttakPeriodeType) {
            kladd.uttakPeriodeType = uttakPeriodeType;
            return this;
        }

        public Builder medUtsettelseÅrsak(UtsettelseÅrsak utsettelseÅrsak) {
            kladd.utsettelseÅrsak = utsettelseÅrsak;
            return this;
        }

        public Builder medOverføringÅrsak(OverføringÅrsak overføringÅrsak) {
            kladd.overføringÅrsak = overføringÅrsak;
            return this;
        }

        public Builder medOppholdÅrsak(OppholdÅrsak oppholdÅrsak) {
            kladd.oppholdÅrsak = oppholdÅrsak;
            return this;
        }

        public Builder medUttakPeriodeVurderingType(UttakPeriodeVurderingType uttakPeriodeVurderingType) {
            kladd.resultat = uttakPeriodeVurderingType;
            return this;
        }

        public Builder medUttakDokumentasjon(List<UttakDokumentasjonDto> uttakDokumentasjon) {
            kladd.dokumentertePerioder = uttakDokumentasjon;
            return this;
        }

        public Builder medArbeidstidsprosent(BigDecimal arbeidstidsprosent) {
            kladd.arbeidstidsprosent = arbeidstidsprosent;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medArbeidsgiver(ArbeidsgiverLagreDto arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidstaker(boolean arbeidstaker) {
            kladd.erArbeidstaker = arbeidstaker;
            return this;
        }

        public Builder medFrilans(boolean frilans) {
            kladd.erFrilanser = frilans;
            return this;
        }

        public Builder medSelvstendig(boolean selvstendig) {
            kladd.erSelvstendig = selvstendig;
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

        public Builder medFlerbarnsdager(boolean flerbarnsdager) {
            kladd.flerbarnsdager = flerbarnsdager;
            return this;
        }

        public Builder medMorsaktivitet(MorsAktivitet morsAktivitet) {
            kladd.morsAktivitet = morsAktivitet;
            return this;
        }

        public Builder medPeriodeKilde(FordelingPeriodeKilde periodeKilde) {
            kladd.periodeKilde = periodeKilde;
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            kladd.mottattDato = mottattDato;
            return this;
        }

        public KontrollerFaktaPeriodeLagreDto build() {
            Objects.requireNonNull(kladd.fom, "fom");
            Objects.requireNonNull(kladd.tom, "tom");
            Objects.requireNonNull(kladd.uttakPeriodeType, "uttakPeriodeType");
            return kladd;
        }
    }
}
