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

    @Column(name = "mottatt_dato")
    private LocalDate mottattDato;

    @Column(name = "mottatt_dato_temp")
    private LocalDate mottattDatoTemp;

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

    public LocalDate getTom() {
        return periode.getTomDato();
    }

    void setPeriode(LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public BigDecimal getArbeidsprosent() {
        return arbeidsprosent;
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
        } else if (Objects.equals(årsakType, UtsettelseÅrsak.KODEVERK)) {
            return UtsettelseÅrsak.fraKode(årsak);
        } else if (Objects.equals(årsakType, OverføringÅrsak.KODEVERK)) {
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

    public boolean getErArbeidstaker() {
        return erArbeidstaker;
    }

    void setErArbeidstaker(boolean erArbeidstaker) {
        this.erArbeidstaker = erArbeidstaker;
    }

    public boolean getErFrilanser() {
        return erFrilanser;
    }

    void setErFrilanser(boolean erFrilanser) {
        this.erFrilanser = erFrilanser;
    }

    public boolean getErSelvstendig() {
        return erSelvstendig;
    }

    void setErSelvstendig(boolean erSelvstendig) {
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

    public boolean erGradert() {
        return getArbeidsprosent() != null && getArbeidsprosent().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean erOpphold() {
        return getÅrsak() instanceof OppholdÅrsak;
    }

    public boolean erOverføring() {
        return getÅrsak() instanceof OverføringÅrsak;
    }

    public boolean erUtsettelse() {
        return getÅrsak() instanceof UtsettelseÅrsak;
    }

    public LocalDate getMottattDato() {
        return mottattDatoTemp;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
        this.mottattDatoTemp = mottattDato;
    }

    /**
     * Kommer denne perioden fra uttaksresultatet til et tidligere vedtak
     */
    public boolean erVedtaksperiode() {
        return getPeriodeKilde().equals(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);
    }

    public boolean erOmsluttetAv(OppgittPeriodeEntitet periode2) {
        return !periode2.getFom().isAfter(getFom()) && !periode2.getTom().isBefore(getTom());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OppgittPeriodeEntitet)) {
            return false;
        }
        OppgittPeriodeEntitet that = (OppgittPeriodeEntitet) o;
        return Objects.equals(uttakPeriodeType, that.uttakPeriodeType) &&
                Objects.equals(årsakType, that.årsakType) &&
                Objects.equals(årsak, that.årsak) &&
                Objects.equals(periode, that.periode) &&
                Objects.equals(arbeidsprosent, that.arbeidsprosent) &&
                Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
                Objects.equals(erArbeidstaker, that.erArbeidstaker) &&
                Objects.equals(morsAktivitet, that.morsAktivitet) &&
                Objects.equals(samtidigUttak, that.samtidigUttak) &&
                Objects.equals(periodeKilde, that.periodeKilde) &&
                Objects.equals(mottattDato, that.mottattDato) &&
                Objects.equals(samtidigUttaksprosent, that.samtidigUttaksprosent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uttakPeriodeType, årsakType, årsak, periode, arbeidsprosent, morsAktivitet, erArbeidstaker,
            arbeidsgiver, periodeKilde, samtidigUttaksprosent, mottattDato);
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
