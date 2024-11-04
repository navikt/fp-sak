package no.nav.foreldrepenger.domene.uttak;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.OPPHØR_MEDLEMSKAP;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class ForeldrepengerUttakPeriode {

    private List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = List.of();
    private LocalDateInterval tidsperiode;
    private boolean samtidigUttak;
    private SamtidigUttaksprosent samtidigUttaksprosent;
    private boolean flerbarnsdager;
    private boolean graderingInnvilget;
    private UttakUtsettelseType utsettelseType = UttakUtsettelseType.UDEFINERT;
    private PeriodeResultatType resultatType;
    private PeriodeResultatÅrsak resultatÅrsak = PeriodeResultatÅrsak.UKJENT;
    private GraderingAvslagÅrsak graderingAvslagÅrsak = GraderingAvslagÅrsak.UKJENT;
    private ManuellBehandlingÅrsak manuellBehandlingÅrsak = ManuellBehandlingÅrsak.UKJENT;
    private OppholdÅrsak oppholdÅrsak = OppholdÅrsak.UDEFINERT;
    private OverføringÅrsak overføringÅrsak;
    private UttakPeriodeType søktKonto;
    private boolean opprinneligSendtTilManuellBehandling;
    private String begrunnelse;
    private boolean manueltBehandlet;
    private LocalDate mottattDato;
    private LocalDate tidligstMottatttDato;
    private MorsAktivitet morsAktivitet;
    private boolean erFraSøknad = true;
    private DokumentasjonVurdering dokumentasjonVurdering;

    private ForeldrepengerUttakPeriode() {

    }

    public boolean erLikBortsettFraTrekkdager(ForeldrepengerUttakPeriode periode) {
        if (periode.getAktiviteter().size() != aktiviteter.size()) {
            return false;
        }
        var likeAktivitieter = periode.getAktiviteter().stream()
            .allMatch(a1 -> getAktiviteter().stream().anyMatch(a1::likBortsettFraTrekkdager));
        return Objects.equals(periode.getTidsperiode(), getTidsperiode())
            && harLikeVerdier(periode)
            && likeAktivitieter;
    }

    public boolean erLikBortsettFraPeriode(ForeldrepengerUttakPeriode periode) {
        if (periode.getAktiviteter().size() != getAktiviteter().size()) {
            return false;
        }
        var likeAktivitieter = periode.getAktiviteter().stream()
            .allMatch(a1 -> getAktiviteter().stream().anyMatch(a1::likEllerSammeAktivitetZeroTrekkdager));
        return harLikeVerdier(periode) && likeAktivitieter;
    }

    private boolean harLikeVerdier(ForeldrepengerUttakPeriode periode) {
        return Objects.equals(periode.getResultatType(), getResultatType())
            && Objects.equals(periode.getResultatÅrsak(), getResultatÅrsak())
            && Objects.equals(periode.isSamtidigUttak(), isSamtidigUttak())
            && Objects.equals(periode.getSamtidigUttaksprosent(), getSamtidigUttaksprosent())
            && Objects.equals(periode.isGraderingInnvilget(), isGraderingInnvilget())
            && Objects.equals(periode.getGraderingAvslagÅrsak(), getGraderingAvslagÅrsak())
            && Objects.equals(periode.isFlerbarnsdager(), isFlerbarnsdager())
            && Objects.equals(periode.getUtsettelseType(), getUtsettelseType())
            && Objects.equals(periode.getOverføringÅrsak(), getOverføringÅrsak())
            && Objects.equals(periode.erFraSøknad(), erFraSøknad())
            && Objects.equals(periode.getOppholdÅrsak(), getOppholdÅrsak());
    }

    public LocalDateInterval getTidsperiode() {
        return tidsperiode;
    }

    public List<ForeldrepengerUttakPeriodeAktivitet> getAktiviteter() {
        return aktiviteter;
    }

    public PeriodeResultatType getResultatType() {
        return resultatType;
    }

    public UttakUtsettelseType getUtsettelseType() {
        return utsettelseType == null ? UttakUtsettelseType.UDEFINERT : utsettelseType;
    }

    public PeriodeResultatÅrsak getResultatÅrsak() {
        return resultatÅrsak;
    }

    public GraderingAvslagÅrsak getGraderingAvslagÅrsak() {
        if (graderingAvslagÅrsak == null || graderingInnvilget) {
            return GraderingAvslagÅrsak.UKJENT;
        }
        return graderingAvslagÅrsak;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public OppholdÅrsak getOppholdÅrsak() {
        return oppholdÅrsak == null ? OppholdÅrsak.UDEFINERT : oppholdÅrsak;
    }

    public OverføringÅrsak getOverføringÅrsak() {
        return overføringÅrsak;
    }

    public boolean isSamtidigUttak() {
        return samtidigUttak;
    }

    public SamtidigUttaksprosent getSamtidigUttaksprosent() {
        return samtidigUttaksprosent;
    }

    public boolean isFlerbarnsdager() {
        return flerbarnsdager;
    }

    public boolean isGraderingInnvilget() {
        return graderingInnvilget;
    }

    public LocalDate getFom() {
        return getTidsperiode().getFomDato();
    }

    public LocalDate getTom() {
        return getTidsperiode().getTomDato();
    }

    public boolean isInnvilget() {
        return PeriodeResultatType.INNVILGET.equals(getResultatType());
    }

    public ManuellBehandlingÅrsak getManuellBehandlingÅrsak() {
        return manuellBehandlingÅrsak;
    }

    public UttakPeriodeType getSøktKonto() {
        return søktKonto;
    }

    public boolean isOpprinneligSendtTilManuellBehandling() {
        return opprinneligSendtTilManuellBehandling;
    }

    public Boolean isManueltBehandlet() {
        return manueltBehandlet;
    }

    public boolean harUtbetaling() {
        return getAktiviteter().stream().anyMatch(aktivitet -> aktivitet.getUtbetalingsgrad().harUtbetaling());
    }
    public boolean harRedusertUtbetaling() {
        return getAktiviteter().stream().anyMatch(aktivitet -> aktivitet.getUtbetalingsgrad().erRedusert());
    }

    public boolean harTrekkdager() {
        return getAktiviteter().stream().anyMatch(aktivitet -> aktivitet.getTrekkdager().merEnn0());
    }

    public boolean harAktivtUttak() {
        return harUtbetaling() || harTrekkdager() || isInnvilgetUtsettelse();
    }

    public boolean isInnvilgetUtsettelse() {
        return isUtsettelse() && !harTrekkdager() && !harUtbetaling() && isInnvilget();
    }

    public boolean isInnvilgetOpphold() {
        return isOpphold() && isInnvilget();
    }

    private boolean isUtsettelse() {
        return !Objects.equals(getUtsettelseType(), UttakUtsettelseType.UDEFINERT);
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public LocalDate getTidligstMottatttDato() {
        return tidligstMottatttDato;
    }

    public boolean isOverføringAvslått() {
        return !isInnvilgetOverføring();
    }

    public boolean isSøktOverføring() {
        return getOverføringÅrsak() != null && !OverføringÅrsak.UDEFINERT.equals(getOverføringÅrsak());
    }

    public boolean isSøktGradering() {
        return getAktiviteter().stream().anyMatch(ForeldrepengerUttakPeriodeAktivitet::isSøktGraderingForAktivitetIPeriode);
    }

    public boolean isOpphold() {
        return !OppholdÅrsak.UDEFINERT.equals(getOppholdÅrsak());
    }

    public boolean isInnvilgetOverføring() {
        return isSøktOverføring() && isInnvilget();
    }

    public MorsAktivitet getMorsAktivitet() {
        return morsAktivitet;
    }

    public boolean erFraSøknad() {
        return erFraSøknad;
    }

    public Optional<DokumentasjonVurdering> getDokumentasjonVurdering() {
        return Optional.ofNullable(dokumentasjonVurdering);
    }

    public boolean harAvslagPgaMedlemskap() {
        return OPPHØR_MEDLEMSKAP.equals(getResultatÅrsak());
    }

    public boolean isOpphør() {
        return PeriodeResultatÅrsak.opphørsAvslagÅrsaker().contains(getResultatÅrsak());
    }

    @Override
    public String toString() {
        return "ForeldrepengerUttakPeriode{" +
            "tidsperiode=" + tidsperiode +
            ", samtidigUttak=" + samtidigUttak +
            ", samtidigUttaksprosent=" + samtidigUttaksprosent +
            ", flerbarnsdager=" + flerbarnsdager +
            ", graderingInnvilget=" + graderingInnvilget +
            ", utsettelseType=" + utsettelseType +
            ", resultatType=" + resultatType +
            ", resultatÅrsak=" + resultatÅrsak +
            ", graderingAvslagÅrsak=" + graderingAvslagÅrsak +
            ", manuellBehandlingÅrsak=" + manuellBehandlingÅrsak +
            ", oppholdÅrsak=" + oppholdÅrsak +
            ", overføringÅrsak=" + overføringÅrsak +
            ", søktKonto=" + søktKonto +
            ", opprinneligSendtTilManuellBehandling=" + opprinneligSendtTilManuellBehandling +
            ", manueltBehandlet=" + manueltBehandlet +
            ", mottattDato=" + mottattDato +
            ", tidligstMottatttDato=" + tidligstMottatttDato +
            ", morsAktivitet=" + morsAktivitet +
            ", erFraSøknad=" + erFraSøknad +
            ", dokumentasjonVurdering=" + dokumentasjonVurdering +
            '}';
    }

    public static class Builder {

        private final ForeldrepengerUttakPeriode kladd;

        public Builder() {
            kladd = new ForeldrepengerUttakPeriode();
        }

        public Builder medTidsperiode(LocalDateInterval tidsperiode) {
            kladd.tidsperiode = tidsperiode;
            return this;
        }

        public Builder medTidsperiode(LocalDate fom, LocalDate tom) {
            return medTidsperiode(new LocalDateInterval(fom, tom));
        }

        public Builder medAktiviteter(List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter) {
            kladd.aktiviteter = aktiviteter;
            return this;
        }

        public Builder medResultatType(PeriodeResultatType type) {
            kladd.resultatType = type;
            return this;
        }

        public Builder medResultatÅrsak(PeriodeResultatÅrsak årsak) {
            kladd.resultatÅrsak = årsak;
            return this;
        }

        public Builder medGraderingAvslagÅrsak(GraderingAvslagÅrsak graderingAvslagÅrsak) {
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

        public Builder medSamtidigUttaksprosent(SamtidigUttaksprosent samtidigUttaksprosent) {
            kladd.samtidigUttaksprosent = samtidigUttaksprosent;
            return this;
        }

        public Builder medFlerbarnsdager(boolean flerbarnsdager) {
            kladd.flerbarnsdager = flerbarnsdager;
            return this;
        }

        public Builder medGraderingInnvilget(boolean graderingInnvilget) {
            kladd.graderingInnvilget = graderingInnvilget;
            return this;
        }

        public Builder medUtsettelseType(UttakUtsettelseType utsettelseType) {
            kladd.utsettelseType = utsettelseType;
            return this;
        }

        public Builder medOppholdÅrsak(OppholdÅrsak oppholdÅrsak) {
            kladd.oppholdÅrsak = oppholdÅrsak;
            return this;
        }

        public Builder medOverføringÅrsak(OverføringÅrsak overføringÅrsak) {
            kladd.overføringÅrsak = overføringÅrsak;
            return this;
        }

        public Builder medManuellBehandlingÅrsak(ManuellBehandlingÅrsak årsak) {
            kladd.manuellBehandlingÅrsak = årsak;
            return this;
        }

        public Builder medSøktKonto(UttakPeriodeType søktKonto) {
            kladd.søktKonto = søktKonto;
            return this;
        }

        public Builder medOpprinneligSendtTilManuellBehandling(boolean opprinneligSendtTilManuellBehandling) {
            kladd.opprinneligSendtTilManuellBehandling = opprinneligSendtTilManuellBehandling;
            return this;
        }

        public Builder medManueltBehandlet(boolean manueltBehandlet) {
            kladd.manueltBehandlet = manueltBehandlet;
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            kladd.mottattDato = mottattDato;
            return this;
        }

        public Builder medTidligstMottattDato(LocalDate tidligstMottatttDato) {
            kladd.tidligstMottatttDato = tidligstMottatttDato;
            return this;
        }

        public Builder medMorsAktivitet(MorsAktivitet morsAktivitet) {
            kladd.morsAktivitet = morsAktivitet;
            return this;
        }

        public Builder medErFraSøknad(boolean erFraSøknad) {
            kladd.erFraSøknad = erFraSøknad;
            return this;
        }

        public Builder medDokumentasjonVurdering(DokumentasjonVurdering dokumentasjonVurdering) {
            kladd.dokumentasjonVurdering = dokumentasjonVurdering;
            return this;
        }

        public ForeldrepengerUttakPeriode build() {
            Objects.requireNonNull(kladd.tidsperiode);
            Objects.requireNonNull(kladd.resultatÅrsak);
            return kladd;
        }
    }
}
