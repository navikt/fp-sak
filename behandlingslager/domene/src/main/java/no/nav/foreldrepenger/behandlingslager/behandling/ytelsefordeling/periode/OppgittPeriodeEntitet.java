package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "SoeknadPeriode")
@Table(name = "YF_FORDELING_PERIODE")
public class OppgittPeriodeEntitet extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_YF_FORDELING_PERIODE")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ChangeTracked
    @Column(name = "periode_type", updatable = false, nullable = false)
    @Convert(converter = UttakPeriodeType.KodeverdiConverter.class)
    private UttakPeriodeType uttakPeriodeType = UttakPeriodeType.UDEFINERT;

    @ChangeTracked
    @Column(name = "vurdering_type", updatable = false, nullable = false)
    @Convert(converter = UttakPeriodeVurderingType.KodeverdiConverter.class)
    private UttakPeriodeVurderingType periodeVurderingType = UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "arbeidstaker", nullable = false)
    @ChangeTracked
    private boolean erArbeidstaker;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "frilanser", nullable = false)
    @ChangeTracked
    private boolean erFrilanser;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "selvstendig", nullable = false)
    @ChangeTracked
    private boolean erSelvstendig;

    @Embedded
    @ChangeTracked
    private Arbeidsgiver arbeidsgiver;

    @Column(name = "kl_aarsak_type", nullable = false)
    @ChangeTracked
    private String årsakType = Årsak.UKJENT.getKodeverk();

    @ChangeTracked
    @Column(name = "aarsak_type", nullable = false, updatable = false)
    private String årsak = Årsak.UKJENT.getKode();

    @Embedded
    @ChangeTracked
    private DatoIntervallEntitet periode;

    @Column(name = "arbeidsprosent")
    @ChangeTracked
    private BigDecimal arbeidsprosent;

    @Column(name = "begrunnelse")
    @ChangeTracked
    private String begrunnelse;

    @ChangeTracked
    @Column(name = "MORS_AKTIVITET", updatable = false, nullable = false)
    @Convert(converter = MorsAktivitet.KodeverdiConverter.class)
    private MorsAktivitet morsAktivitet = MorsAktivitet.UDEFINERT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fordeling_id", nullable = false, updatable = false, unique = true)
    private OppgittFordelingEntitet oppgittFordeling;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "samtidig_uttak", nullable = false)
    @ChangeTracked
    private boolean samtidigUttak;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "flerbarnsdager", nullable = false)
    @ChangeTracked
    private boolean flerbarnsdager;

    @Column(name = "samtidig_uttaksprosent")
    @ChangeTracked
    private SamtidigUttaksprosent samtidigUttaksprosent;

    @Column(name = "FORDELING_PERIODE_KILDE", nullable = false, updatable = false)
    @Convert(converter = FordelingPeriodeKilde.KodeverdiConverter.class)
    private FordelingPeriodeKilde periodeKilde = FordelingPeriodeKilde.SØKNAD;

    //Hvis bruker søker om en periode flere ganger så oppdateres mottattDato med ny dato, men tidligst mottatt dato settes til
    //datoen bruker først søkte om perioden. Så tidligstMottattDato kan ligge før mottattDato ved flere søknader
    @Column(name = "mottatt_dato")
    private LocalDate mottattDato;
    @Column(name = "tidligst_mottatt_dato")
    private LocalDate tidligstMottattDato;

    protected OppgittPeriodeEntitet() {
        // Hibernate
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(uttakPeriodeType, årsakType, årsak, arbeidsgiver, periode);
    }

    public Long getId() {
        return id;
    }

    public UttakPeriodeType getPeriodeType() {
        return uttakPeriodeType;
    }

    void setPeriodeType(UttakPeriodeType uttakPeriodeType) {
        this.uttakPeriodeType = uttakPeriodeType;
    }

    void setOppgittFordeling(OppgittFordelingEntitet oppgittFordeling) {
        this.oppgittFordeling = oppgittFordeling;
    }

    public LocalDate getFom() {
        return periode.getFomDato();
    }

    public DatoIntervallEntitet getTidsperiode() {
        return periode;
    }

    public LocalDate getTom() {
        return periode.getTomDato();
    }

    void setPeriode(LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public BigDecimal getArbeidsprosent() {
        return arbeidsprosent;
    }

    public Stillingsprosent getArbeidsprosentSomStillingsprosent() {
        return new Stillingsprosent(arbeidsprosent);
    }

    void setArbeidsprosent(BigDecimal arbeidsprosent) {
        this.arbeidsprosent = arbeidsprosent;
    }

    void setÅrsakType(String årsakType) {
        this.årsakType = årsakType;
    }

    public Årsak getÅrsak() {
        if (Objects.equals(årsakType, OppholdÅrsak.KODEVERK)) {
            return OppholdÅrsak.fraKode(årsak);
        }
        if (Objects.equals(årsakType, UtsettelseÅrsak.KODEVERK)) {
            return UtsettelseÅrsak.fraKode(årsak);
        }
        if (Objects.equals(årsakType, OverføringÅrsak.KODEVERK)) {
            return OverføringÅrsak.fraKode(årsak);
        }
        return Årsak.UKJENT;
    }

    void setÅrsak(Årsak årsak) {
        this.årsak = årsak.getKode();
    }

    public MorsAktivitet getMorsAktivitet() {
        return morsAktivitet;
    }

    void setMorsAktivitet(MorsAktivitet morsAktivitet) {
        this.morsAktivitet = morsAktivitet;
    }

    public Optional<String> getBegrunnelse() {
        return Optional.ofNullable(begrunnelse);
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public boolean isArbeidstaker() {
        return erArbeidstaker;
    }

    void setArbeidstaker(boolean erArbeidstaker) {
        this.erArbeidstaker = erArbeidstaker;
    }

    public boolean isFrilanser() {
        return erFrilanser;
    }

    void setFrilanser(boolean erFrilanser) {
        this.erFrilanser = erFrilanser;
    }

    public boolean isSelvstendig() {
        return erSelvstendig;
    }

    void setSelvstendig(boolean erSelvstendig) {
        this.erSelvstendig = erSelvstendig;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public UttakPeriodeVurderingType getPeriodeVurderingType() {
        return periodeVurderingType;
    }

    void setPeriodeVurderingType(UttakPeriodeVurderingType periodeVurderingType) {
        this.periodeVurderingType = periodeVurderingType;
    }

    public boolean isSamtidigUttak() {
        return samtidigUttak;
    }

    void setSamtidigUttak(boolean samtidigUttak) {
        this.samtidigUttak = samtidigUttak;
    }

    public SamtidigUttaksprosent getSamtidigUttaksprosent() {
        return isSamtidigUttak() ? samtidigUttaksprosent : null;
    }

    void setSamtidigUttaksprosent(SamtidigUttaksprosent samtidigUttaksprosent) {
        this.samtidigUttaksprosent = samtidigUttaksprosent;
    }

    public boolean isFlerbarnsdager() {
        return flerbarnsdager;
    }

    void setFlerbarnsdager(boolean flerbarnsdager) {
        this.flerbarnsdager = flerbarnsdager;
    }

    public FordelingPeriodeKilde getPeriodeKilde() {
        return periodeKilde;
    }

    void setPeriodeKilde(FordelingPeriodeKilde periodeKilde) {
        this.periodeKilde = periodeKilde;
    }

    public boolean isGradert() {
        return getArbeidsprosent() != null && getArbeidsprosent().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isOpphold() {
        return getÅrsak() instanceof OppholdÅrsak;
    }

    public boolean isOverføring() {
        return getÅrsak() instanceof OverføringÅrsak;
    }

    public boolean isUtsettelse() {
        return getÅrsak() instanceof UtsettelseÅrsak;
    }

    public GraderingAktivitetType utledGraderingAktivitetType() {
        if (erArbeidstaker) {
            return GraderingAktivitetType.ARBEID;
        } else if (erSelvstendig) {
            return GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
        } else if (erFrilanser) {
            return GraderingAktivitetType.FRILANS;
        } else {
            return null;
        }
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public Optional<LocalDate> getTidligstMottattDato() {
        return Optional.ofNullable(tidligstMottattDato);
    }

    public void setTidligstMottattDato(LocalDate tidligstMottattDato) {
        this.tidligstMottattDato = tidligstMottattDato;
    }

    /**
     * Kommer denne perioden fra uttaksresultatet til et tidligere vedtak
     */
    public boolean isVedtaksperiode() {
        return getPeriodeKilde().equals(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OppgittPeriodeEntitet that)) {
            return false;
        }
        return Objects.equals(uttakPeriodeType, that.uttakPeriodeType) &&
                Objects.equals(årsakType, that.årsakType) &&
                Objects.equals(årsak, that.årsak) &&
                Objects.equals(periode, that.periode) &&
                Objects.equals(getArbeidsprosentSomStillingsprosent(), that.getArbeidsprosentSomStillingsprosent()) &&
                Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
                Objects.equals(erArbeidstaker, that.erArbeidstaker) &&
                Objects.equals(morsAktivitet, that.morsAktivitet) &&
                Objects.equals(samtidigUttak, that.samtidigUttak) &&
                Objects.equals(periodeKilde, that.periodeKilde) &&
                Objects.equals(mottattDato, that.mottattDato) &&
                Objects.equals(tidligstMottattDato, that.tidligstMottattDato) &&
                Objects.equals(samtidigUttaksprosent, that.samtidigUttaksprosent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uttakPeriodeType, årsakType, årsak, periode, arbeidsprosent, morsAktivitet, erArbeidstaker,
            arbeidsgiver, periodeKilde, samtidigUttaksprosent, mottattDato, tidligstMottattDato);
    }

    @Override
    public String toString() {
        return "OppgittPeriodeEntitet{" +
                "uttakPeriodeType=" + uttakPeriodeType.getKode() +
                ", årsak=" + årsak +
                ", periode=" + periode +
                '}';
    }
}
