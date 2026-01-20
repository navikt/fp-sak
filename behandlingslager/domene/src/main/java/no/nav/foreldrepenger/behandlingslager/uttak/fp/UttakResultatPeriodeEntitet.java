package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.BatchSize;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity
@Table(name = "UTTAK_RESULTAT_PERIODE")
public class UttakResultatPeriodeEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_RESULTAT_PERIODE")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uttak_resultat_perioder_id", nullable = false, updatable = false, unique = true)
    private UttakResultatPerioderEntitet perioder;

    @OneToMany(mappedBy = "periode")
    @BatchSize(size = 25)
    private List<UttakResultatPeriodeAktivitetEntitet> aktiviteter = new ArrayList<>();

    /**
     * Er egentlig en-til-en, men gjort om til onetomany for å force lazy fetching. Funket ikke med lazy og en-til-en
     */
    @OneToMany(mappedBy = "periode")
    private List<UttakResultatDokRegelEntitet> dokRegel = new ArrayList<>();

    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false))
    @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    private DatoIntervallEntitet tidsperiode;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "gradering_innvilget", nullable = false)
    private boolean graderingInnvilget;

    @Convert(converter = UttakUtsettelseType.KodeverdiConverter.class)
    @Column(name = "uttak_utsettelse_type", nullable = false, updatable = false)
    private UttakUtsettelseType utsettelseType = UttakUtsettelseType.UDEFINERT;

    @Convert(converter = OppholdÅrsak.KodeverdiConverter.class)
    @Column(name = "opphold_aarsak", nullable = false, updatable = false)
    private OppholdÅrsak oppholdÅrsak = OppholdÅrsak.UDEFINERT;

    @Convert(converter = OverføringÅrsak.KodeverdiConverter.class)
    @Column(name = "overfoering_aarsak", nullable = false, updatable = false)
    private OverføringÅrsak overføringÅrsak = OverføringÅrsak.UDEFINERT;

    @Enumerated(EnumType.STRING)
    @Column(name="periode_resultat_type", nullable = false)
    private PeriodeResultatType periodeResultatType;

    @Convert(converter = PeriodeResultatÅrsak.KodeverdiConverter.class)
    @Column(name="PERIODE_RESULTAT_AARSAK", nullable = false)
    private PeriodeResultatÅrsak periodeResultatÅrsak;

    @Convert(converter = GraderingAvslagÅrsak.KodeverdiConverter.class)
    @Column(name="gradering_avslag_aarsak", nullable = false)
    private GraderingAvslagÅrsak graderingAvslagÅrsak = GraderingAvslagÅrsak.UKJENT;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "flerbarnsdager", nullable = false)
    private boolean flerbarnsdager;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "samtidig_uttak", nullable = false)
    private boolean samtidigUttak;

    @Column(name = "samtidig_uttaksprosent")
    private SamtidigUttaksprosent samtidigUttaksprosent;

    @ManyToOne
    @JoinColumn(name = "periode_soknad_id", updatable = false)
    private UttakResultatPeriodeSøknadEntitet periodeSøknad;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "manuelt_behandlet", nullable = false, updatable = false)
    private boolean manueltBehandlet;

    @Column(name = "manuell_behandling_aarsak", updatable = false, nullable = false)
    @Convert(converter = ManuellBehandlingÅrsak.KodeverdiConverter.class)
    private ManuellBehandlingÅrsak manuellBehandlingÅrsak = ManuellBehandlingÅrsak.UKJENT;

    @Override
    public String toString() {
        return "UttakResultatPeriodeEntitet{" +
            "tidsperiode=" + tidsperiode +
            ", graderingInnvilget=" + graderingInnvilget +
            ", utsettelseType=" + utsettelseType.getKode() +
            ", oppholdÅrsak=" + oppholdÅrsak.getKode() +
            ", overføringÅrsak=" + overføringÅrsak.getKode() +
            ", periodeResultatType=" + periodeResultatType.getKode() +
            ", periodeResultatÅrsak=" + periodeResultatÅrsak +
            ", samtidigUttak=" + samtidigUttak +
            ", samtidigUttaksprosent=" + samtidigUttaksprosent +
            ", manuellBehandlingÅrsak=" + manuellBehandlingÅrsak.getKode() +
            ", manueltBehandlet=" + manueltBehandlet +
            '}';
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFom() {
        return tidsperiode.getFomDato();
    }

    public LocalDate getTom() {
        return tidsperiode.getTomDato();
    }

    public DatoIntervallEntitet getTidsperiode() {
        return tidsperiode;
    }

    public UttakUtsettelseType getUtsettelseType() {
        return utsettelseType;
    }

    public OppholdÅrsak getOppholdÅrsak() {
        return oppholdÅrsak;
    }

    public PeriodeResultatType getResultatType() {
        return periodeResultatType;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public PeriodeResultatÅrsak getResultatÅrsak() {
        return periodeResultatÅrsak;
    }

    public GraderingAvslagÅrsak getGraderingAvslagÅrsak() {
        return graderingAvslagÅrsak;
    }

    public UttakResultatDokRegelEntitet getDokRegel() {
        return dokRegel.isEmpty() ? null : dokRegel.getFirst();
    }

    public boolean isSamtidigUttak() {
        return samtidigUttak;
    }

    public SamtidigUttaksprosent getSamtidigUttaksprosent() {
        return isSamtidigUttak() ? samtidigUttaksprosent : null;
    }

    public boolean isFlerbarnsdager() {
        return flerbarnsdager;
    }

    public boolean isGraderingInnvilget() {
        return graderingInnvilget;
    }

    public OverføringÅrsak getOverføringÅrsak() {
        return overføringÅrsak;
    }

    public void leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet aktivitet) {
        if (!aktiviteter.contains(aktivitet)) {
            this.aktiviteter.add(aktivitet);
            aktivitet.setPeriode(this);
        }
    }

    public List<UttakResultatPeriodeAktivitetEntitet> getAktiviteter() {
        return aktiviteter;
    }

    public void setPerioder(UttakResultatPerioderEntitet perioder) {
        this.perioder = perioder;
    }

    public ManuellBehandlingÅrsak getManuellBehandlingÅrsak() {
        return manuellBehandlingÅrsak;
    }

    public Optional<UttakResultatPeriodeSøknadEntitet> getPeriodeSøknad() {
        return Optional.ofNullable(periodeSøknad);
    }

    public boolean isManueltBehandlet() {
        return manueltBehandlet;
    }

    public boolean overlapper(LocalDate dato) {
        Objects.requireNonNull(dato);
        return !dato.isBefore(getFom()) && !dato.isAfter(getTom());
    }

    public boolean isInnvilget() {
        return Objects.equals(getResultatType(), PeriodeResultatType.INNVILGET);
    }

    public boolean isOpphold() {
        return !Objects.equals(getOppholdÅrsak(), OppholdÅrsak.UDEFINERT);
    }

    public boolean isUtsettelse() {
        return !Objects.equals(getUtsettelseType(), UttakUtsettelseType.UDEFINERT);
    }

    public boolean isOverføring() {
        return !Objects.equals(getOverføringÅrsak(), OverføringÅrsak.UDEFINERT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (UttakResultatPeriodeEntitet) o;
        return Objects.equals(perioder, that.perioder) &&
            Objects.equals(tidsperiode, that.tidsperiode);
    }

    @Override
    public int hashCode() {

        return Objects.hash(perioder, tidsperiode);
    }

    public static class Builder {
        private final UttakResultatPeriodeEntitet kladd;

        public Builder(LocalDate fom, LocalDate tom) {
            this.kladd = new UttakResultatPeriodeEntitet();
            this.kladd.tidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        }

        public Builder medGraderingInnvilget(boolean innvilget) {
            kladd.graderingInnvilget = innvilget;
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

        public Builder medManueltBehandlet(boolean manueltBehandlet) {
            kladd.manueltBehandlet = manueltBehandlet;
            return this;
        }

        public Builder medResultatType(PeriodeResultatType periodeResultatType, PeriodeResultatÅrsak periodeResultatÅrsak) {
            kladd.periodeResultatType = periodeResultatType;
            kladd.periodeResultatÅrsak = periodeResultatÅrsak;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medManuellBehandlingÅrsak(ManuellBehandlingÅrsak manuellBehandlingÅrsak) {
            kladd.manuellBehandlingÅrsak = manuellBehandlingÅrsak;
            return this;
        }

        public Builder medDokRegel(UttakResultatDokRegelEntitet dokRegel) {
            kladd.dokRegel = Collections.singletonList(dokRegel);
            return this;
        }

        public Builder medPeriodeSoknad(UttakResultatPeriodeSøknadEntitet periodeSøknad) {
            kladd.periodeSøknad = periodeSøknad;
            return this;
        }

        public Builder medGraderingAvslagÅrsak(GraderingAvslagÅrsak graderingAvslagÅrsak) {
            kladd.graderingAvslagÅrsak = graderingAvslagÅrsak;
            return this;
        }

        public UttakResultatPeriodeEntitet build() {
            Objects.requireNonNull(kladd.tidsperiode, "tidsperiode");
            Objects.requireNonNull(kladd.periodeResultatType, "periodeResultatType");
            if (!kladd.dokRegel.isEmpty()) {
                kladd.dokRegel.getFirst().setPeriode(kladd);
            }
            if (kladd.utsettelseType == null) {
                kladd.utsettelseType = UttakUtsettelseType.UDEFINERT;
            }
            if (kladd.oppholdÅrsak == null) {
                kladd.oppholdÅrsak = OppholdÅrsak.UDEFINERT;
            }
            if (kladd.overføringÅrsak == null) {
                kladd.overføringÅrsak = OverføringÅrsak.UDEFINERT;
            }
            return kladd;
        }

    }
}
