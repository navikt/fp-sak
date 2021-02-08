
package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

/**
 * Denne klassen er en ren avbildning fra Oppdragsløsningens meldingsformater.
 * Navngivning følger ikke nødvendigvis Vedtaksløsningens navnestandarder.
 */
@Immutable
@Entity(name = "Oppdragslinje150")
@Table(name = "OKO_OPPDRAG_LINJE_150")
public class Oppdragslinje150 extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OKO_OPPDRAG_LINJE_150")
    private Long id;

    @Column(name = "kode_endring_linje", nullable = false)
    private String kodeEndringLinje;

    @Column(name = "vedtak_id", nullable = false)
    private String vedtakId;

    @Column(name = "delytelse_id", nullable = false)
    private Long delytelseId;

    @Column(name = "kode_klassifik", nullable = false)
    private String kodeKlassifik;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "dato_vedtak_fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "dato_vedtak_tom", nullable = false))
    })
    private DatoIntervallEntitet vedtakPeriode;

    @Column(name = "sats", nullable = false)
    private long sats;

    @Column(name = "type_sats", nullable = false)
    private String typeSats;

    @ManyToOne(optional = false)
    @JoinColumn(name = "oppdrag110_id", nullable = false)
    private Oppdrag110 oppdrag110;

    @Embedded
    private Utbetalingsgrad utbetalingsgrad;

    @Column(name = "kode_status_linje")
    private String kodeStatusLinje;

    @Column(name = "dato_status_fom")
    private LocalDate datoStatusFom;

    @Column(name = "utbetales_til_id")
    private String utbetalesTilId;

    @Column(name = "ref_delytelse_id")
    private Long refDelytelseId;

    @Column(name = "ref_fagsystem_id")
    private Long refFagsystemId;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "oppdragslinje150")
    private Refusjonsinfo156 refusjonsinfo156;

    private Oppdragslinje150() {}

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKodeEndringLinje() {
        return kodeEndringLinje;
    }

    public String getKodeStatusLinje() {
        return kodeStatusLinje;
    }

    public boolean gjelderOpphør() {
        return getKodeStatusLinje() != null;
    }

    public LocalDate getDatoStatusFom() {
        return datoStatusFom;
    }

    public String getVedtakId() {
        return vedtakId;
    }

    public Long getDelytelseId() {
        return delytelseId;
    }

    public String getKodeKlassifik() {
        return kodeKlassifik;
    }

    public ØkonomiKodeKlassifik getKodeKlassifikEnum() {
        return ØkonomiKodeKlassifik.fraKode(kodeKlassifik);
    }

    public LocalDate getDatoVedtakFom() {
        return vedtakPeriode.getFomDato();
    }

    public LocalDate getDatoVedtakTom() {
        return vedtakPeriode.getTomDato();
    }

    public long getSats() {
        return sats;
    }

    public void setSats(long sats) {
        this.sats = sats;
    }

    public String getTypeSats() {
        return typeSats;
    }

    public String getUtbetalesTilId() {
        return utbetalesTilId;
    }

    public Long getRefFagsystemId() {
        return refFagsystemId;
    }

    public Long getRefDelytelseId() {
        return refDelytelseId;
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public Oppdrag110 getOppdrag110() {
        return oppdrag110;
    }

    public void setOppdrag110(Oppdrag110 oppdrag110) {
        this.oppdrag110 = oppdrag110;
    }

    public Refusjonsinfo156 getRefusjonsinfo156() {
        return refusjonsinfo156;
    }

    protected void setRefusjonsinfo156(Refusjonsinfo156 refusjonsinfo156) {
        Objects.requireNonNull(refusjonsinfo156, "refusjonsinfo156");
        this.refusjonsinfo156 = refusjonsinfo156;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Oppdragslinje150)) {
            return false;
        }
        Oppdragslinje150 oppdrlinje150 = (Oppdragslinje150) object;
        return Objects.equals(kodeEndringLinje, oppdrlinje150.getKodeEndringLinje())
            && Objects.equals(kodeStatusLinje, oppdrlinje150.getKodeStatusLinje())
            && Objects.equals(datoStatusFom, oppdrlinje150.getDatoStatusFom())
            && Objects.equals(vedtakId, oppdrlinje150.getVedtakId())
            && Objects.equals(delytelseId, oppdrlinje150.getDelytelseId())
            && Objects.equals(kodeKlassifik, oppdrlinje150.getKodeKlassifik())
            && Objects.equals(vedtakPeriode, oppdrlinje150.vedtakPeriode)
            && Objects.equals(sats, oppdrlinje150.getSats())
            && Objects.equals(typeSats, oppdrlinje150.getTypeSats())
            && Objects.equals(utbetalesTilId, oppdrlinje150.getUtbetalesTilId())
            && Objects.equals(refFagsystemId, oppdrlinje150.getRefFagsystemId())
            && Objects.equals(refDelytelseId, oppdrlinje150.getRefDelytelseId())
            && Objects.equals(utbetalingsgrad, oppdrlinje150.getUtbetalingsgrad());
    }

    @Override
    public int hashCode() {
        return Objects.hash(kodeEndringLinje, kodeStatusLinje, datoStatusFom, vedtakId, delytelseId, kodeKlassifik,
            vedtakPeriode, sats, typeSats, utbetalesTilId, refFagsystemId, refDelytelseId, utbetalingsgrad);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$
            + "kodeEndringLinje=" + kodeEndringLinje + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "kodeStatusLinje=" + kodeStatusLinje + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "datoStatusFom=" + datoStatusFom + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "vedtakId=" + vedtakId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "delytelseId=" + delytelseId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "kodeKlassifik=" + kodeKlassifik + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "vedtakPeriode=" + vedtakPeriode + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "sats=" + sats + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "typeSats=" + typeSats + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "utbetalesTilId=" + utbetalesTilId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "refFagsystemId=" + refFagsystemId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "refDelytelseId=" + refDelytelseId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "grad=" + utbetalingsgrad + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "opprettetTs=" + getOpprettetTidspunkt() //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
    }

    public static class Builder {
        private String kodeEndringLinje;
        private String kodeStatusLinje;
        private LocalDate datoStatusFom;
        private String vedtakId;
        private Long delytelseId;
        private String kodeKlassifik;
        private DatoIntervallEntitet vedtakPeriode;
        private Long sats;
        private String typeSats;
        private String utbetalesTilId;
        private Long refFagsystemId;
        private Long refDelytelseId;
        private Oppdrag110 oppdrag110;
        private Utbetalingsgrad utbetalingsgrad;

        public Builder medKodeEndringLinje(String kodeEndringLinje) {
            this.kodeEndringLinje = kodeEndringLinje;
            return this;
        }

        public Builder medKodeStatusLinje(String kodeStatusLinje) {
            this.kodeStatusLinje = kodeStatusLinje;
            return this;
        }

        public Builder medDatoStatusFom(LocalDate datoStatusFom) {
            this.datoStatusFom = datoStatusFom;
            return this;
        }

        public Builder medVedtakId(String vedtakId) {
            this.vedtakId = vedtakId;
            return this;
        }

        public Builder medDelytelseId(Long delytelseId) {
            this.delytelseId = delytelseId;
            return this;
        }

        public Builder medKodeKlassifik(String kodeKlassifik) {
            this.kodeKlassifik = kodeKlassifik;
            return this;
        }

        public Builder medVedtakFomOgTom(LocalDate datoVedtakFom, LocalDate datoVedtakTom) {
            this.vedtakPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(datoVedtakFom, datoVedtakTom);
            return this;
        }

        public Builder medSats(long sats) {
            this.sats = sats;
            return this;
        }

        public Builder medTypeSats(String typeSats) {
            this.typeSats = typeSats;
            return this;
        }

        public Builder medUtbetalesTilId(String utbetalesTilId) {
            this.utbetalesTilId = utbetalesTilId;
            return this;
        }

        public Builder medRefFagsystemId(Long refFagsystemId) {
            this.refFagsystemId = refFagsystemId;
            return this;
        }

        public Builder medRefDelytelseId(Long refDelytelseId) {
            this.refDelytelseId = refDelytelseId;
            return this;
        }

        public Builder medUtbetalingsgrad(Utbetalingsgrad utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medOppdrag110(Oppdrag110 oppdrag110) {
            this.oppdrag110 = oppdrag110;
            return this;
        }

        public Oppdragslinje150 build() {
            verifyStateForBuild();
            Oppdragslinje150 oppdragslinje150 = new Oppdragslinje150();
            oppdragslinje150.kodeEndringLinje = kodeEndringLinje;
            oppdragslinje150.kodeStatusLinje = kodeStatusLinje;
            oppdragslinje150.datoStatusFom = datoStatusFom;
            oppdragslinje150.vedtakId = vedtakId;
            oppdragslinje150.delytelseId = delytelseId;
            oppdragslinje150.kodeKlassifik = kodeKlassifik;
            oppdragslinje150.vedtakPeriode = vedtakPeriode;
            oppdragslinje150.sats = sats;
            oppdragslinje150.typeSats = typeSats;
            oppdragslinje150.utbetalesTilId = utbetalesTilId;
            oppdragslinje150.refFagsystemId = refFagsystemId;
            oppdragslinje150.refDelytelseId = refDelytelseId;
            oppdragslinje150.utbetalingsgrad = utbetalingsgrad;
            oppdragslinje150.oppdrag110 = oppdrag110;
            oppdrag110.addOppdragslinje150(oppdragslinje150);

            return oppdragslinje150;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(kodeEndringLinje, "kodeEndringLinje");
            Objects.requireNonNull(kodeKlassifik, "kodeKlassifik");
            Objects.requireNonNull(vedtakPeriode, "vedtakPeriode");
            Objects.requireNonNull(sats, "sats");
            Objects.requireNonNull(typeSats, "typeSats");
            Objects.requireNonNull(oppdrag110, "oppdrag110");
        }
    }
}
