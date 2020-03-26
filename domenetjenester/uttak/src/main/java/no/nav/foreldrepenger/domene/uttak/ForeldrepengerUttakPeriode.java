package no.nav.foreldrepenger.domene.uttak;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakUtsettelseType;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class ForeldrepengerUttakPeriode {

    private List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = List.of();
    private LocalDateInterval tidsperiode;
    private boolean samtidigUttak;
    private BigDecimal samtidigUttaksprosent;
    private boolean flerbarnsdager;
    private boolean graderingInnvilget;
    private UttakUtsettelseType utsettelseType = UttakUtsettelseType.UDEFINERT;
    private PeriodeResultatType resultatType;
    private PeriodeResultatÅrsak resultatÅrsak = PeriodeResultatÅrsak.UKJENT;
    private GraderingAvslagÅrsak graderingAvslagÅrsak = GraderingAvslagÅrsak.UKJENT;
    private ManuellBehandlingÅrsak manuellBehandlingÅrsak = ManuellBehandlingÅrsak.UKJENT;
    private OppholdÅrsak oppholdÅrsak = OppholdÅrsak.UDEFINERT;
    private UttakPeriodeType søktKonto;
    private boolean opprinneligSendtTilManuellBehandling;
    private String begrunnelse;
    private boolean manueltBehandlet;

    private ForeldrepengerUttakPeriode() {

    }

    public boolean erLik(ForeldrepengerUttakPeriode periode) {
        return Objects.equals(periode.getTidsperiode(), getTidsperiode())
            && Objects.equals(periode.getResultatType(), getResultatType())
            && Objects.equals(periode.getResultatÅrsak(), getResultatÅrsak())
            && Objects.equals(periode.isSamtidigUttak(), isSamtidigUttak())
            && Objects.equals(periode.getSamtidigUttaksprosent(), getSamtidigUttaksprosent())
            && Objects.equals(periode.isFlerbarnsdager(), isFlerbarnsdager())
            && Objects.equals(periode.getUtsettelseType(), getUtsettelseType())
            && Objects.equals(periode.getOppholdÅrsak(), getOppholdÅrsak())
            && aktiviteterErLike(periode.getAktiviteter());
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
        } else {
            return graderingAvslagÅrsak;
        }
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public OppholdÅrsak getOppholdÅrsak() {
        return oppholdÅrsak == null ? OppholdÅrsak.UDEFINERT : oppholdÅrsak;
    }

    private boolean aktiviteterErLike(List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter) {
        for (ForeldrepengerUttakPeriodeAktivitet aktivitet : aktiviteter) {
            if (!harLikAktivitet(aktivitet)) {
                return false;
            }
        }
        return true;
    }

    private boolean harLikAktivitet(ForeldrepengerUttakPeriodeAktivitet aktivitet1) {
        for (ForeldrepengerUttakPeriodeAktivitet aktivitet2 : getAktiviteter()) {
            if (aktivitet1.likBortsettFraTrekkdager(aktivitet2)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSamtidigUttak() {
        return samtidigUttak;
    }

    public BigDecimal getSamtidigUttaksprosent() {
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
            ", søktKonto=" + søktKonto +
            ", opprinneligSendtTilManuellBehandling=" + opprinneligSendtTilManuellBehandling +
            ", manueltBehandlet=" + manueltBehandlet +
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

        public Builder medSamtidigUttaksprosent(BigDecimal samtidigUttaksprosent) {
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

        public ForeldrepengerUttakPeriode build() {
            Objects.requireNonNull(kladd.tidsperiode);
            Objects.requireNonNull(kladd.resultatÅrsak);
            return kladd;
        }
    }
}
