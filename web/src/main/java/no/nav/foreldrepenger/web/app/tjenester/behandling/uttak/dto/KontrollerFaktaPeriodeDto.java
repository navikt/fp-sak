package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType.PERIODE_OK;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType.PERIODE_OK_ENDRET;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.KontrollerFaktaPeriode;

public class KontrollerFaktaPeriodeDto {

    private LocalDate tom;

    private LocalDate fom;

    private UttakPeriodeType uttakPeriodeType;

    private UtsettelseÅrsak utsettelseÅrsak;

    private OverføringÅrsak overføringÅrsak;

    private OppholdÅrsak oppholdÅrsak;

    private UttakPeriodeVurderingType resultat;

    private List<UttakDokumentasjonDto> dokumentertePerioder;

    private BigDecimal arbeidstidsprosent;

    private String begrunnelse;

    private boolean bekreftet;

    private String arbeidsgiverReferanse;

    private boolean erArbeidstaker;
    private boolean erFrilanser;
    private boolean erSelvstendig;
    private boolean samtidigUttak;

    private SamtidigUttaksprosent samtidigUttaksprosent;

    private boolean flerbarnsdager;

    private MorsAktivitet morsAktivitet;

    private FordelingPeriodeKilde periodeKilde;

    private LocalDate mottattDato;
    private LocalDate tidligstMottattDato;

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

    public boolean isBekreftet() {
        return bekreftet;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public LocalDate getTidligstMottattDato() {
        return tidligstMottattDato;
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

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
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

    public static class Builder {

        private final KontrollerFaktaPeriodeDto kladd = new KontrollerFaktaPeriodeDto();

        public Builder() {
        }

        public Builder(KontrollerFaktaPeriode periode, String arbeidsgiverReferanse) {
            medPeriode(periode.getOppgittPeriode().getFom(), periode.getOppgittPeriode().getTom());
            medUttakPeriodeType(periode.getOppgittPeriode().getPeriodeType());
            medArbeidstidsprosent(periode.getOppgittPeriode().getArbeidsprosent());
            medBegrunnelse(periode.getOppgittPeriode().getBegrunnelse().orElse(null));
            medBekreftet(periode.erBekreftet());
            medUttakPeriodeVurderingType(periode.getVurdering());
            medArbeidsgiver(arbeidsgiverReferanse);
            medArbeidstaker(periode.getOppgittPeriode().getGraderingAktivitetType() == GraderingAktivitetType.ARBEID);
            medFrilans(periode.getOppgittPeriode().getGraderingAktivitetType() == GraderingAktivitetType.FRILANS);
            medSelvstendig(periode.getOppgittPeriode().getGraderingAktivitetType() == GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE);
            medSamtidigUttak(periode.getOppgittPeriode().isSamtidigUttak());
            medSamtidigUttaksprosent(periode.getOppgittPeriode().getSamtidigUttaksprosent());
            medMorsaktivitet(periode.getOppgittPeriode().getMorsAktivitet());
            medFlerbarnsdager(periode.getOppgittPeriode().isFlerbarnsdager());
            medPeriodeKilde(periode.getOppgittPeriode().getPeriodeKilde());
            medMottattDato(periode.getOppgittPeriode().getMottattDato());
            medTidligstMottattDato(periode.getOppgittPeriode().getTidligstMottattDato().orElse(null));

            if (periode.getOppgittPeriode().isUtsettelse()) {
                medUtsettelseÅrsak((UtsettelseÅrsak) periode.getOppgittPeriode().getÅrsak()); //NOSONAR
            } else if (periode.getOppgittPeriode().isOverføring()) {
                medOverføringÅrsak((OverføringÅrsak) periode.getOppgittPeriode().getÅrsak()); //NOSONAR
            } else if (periode.getOppgittPeriode().isOpphold()) {
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

        public Builder medBekreftet(boolean bekreftet) {
            kladd.bekreftet = bekreftet;
            return this;
        }

        public Builder medArbeidsgiver(String arbeidsgiverReferanse) {
            kladd.arbeidsgiverReferanse = arbeidsgiverReferanse;
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

        public Builder medTidligstMottattDato(LocalDate tidligstMottattDato) {
            kladd.tidligstMottattDato = tidligstMottattDato;
            return this;
        }

        public KontrollerFaktaPeriodeDto build() {
            Objects.requireNonNull(kladd.fom, "fom");
            Objects.requireNonNull(kladd.tom, "tom");
            if (kladd.utsettelseÅrsak == null) {
                kladd.utsettelseÅrsak = UtsettelseÅrsak.UDEFINERT;
            }
            if (kladd.overføringÅrsak == null) {
                kladd.overføringÅrsak = OverføringÅrsak.UDEFINERT;
            }
            if (kladd.oppholdÅrsak == null) {
                kladd.oppholdÅrsak = OppholdÅrsak.UDEFINERT;
            }
            if (kladd.uttakPeriodeType == null) {
                kladd.uttakPeriodeType = UttakPeriodeType.UDEFINERT;
            }
            if (kladd.resultat == null) {
                kladd.resultat = UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT;
            }
            if (kladd.periodeKilde == null) {
                kladd.periodeKilde = FordelingPeriodeKilde.SØKNAD;
            }
            if (kladd.morsAktivitet == null) {
                kladd.morsAktivitet = MorsAktivitet.UDEFINERT;
            }
            return kladd;
        }
    }
}
