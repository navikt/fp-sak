package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "kode_endring_linje", nullable = false)
    private KodeEndringLinje kodeEndringLinje;

    @Column(name = "vedtak_id", nullable = false)
    private String vedtakId;

    @Column(name = "delytelse_id", nullable = false)
    private Long delytelseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kode_klassifik", nullable = false)
    private KodeKlassifik kodeKlassifik;

    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "dato_vedtak_fom", nullable = false))
    @AttributeOverride(name = "tomDato", column = @Column(name = "dato_vedtak_tom", nullable = false))
    private DatoIntervallEntitet vedtakPeriode;

    @Embedded
    @AttributeOverride(name = "sats", column = @Column(name = "sats", nullable = false))
    private Sats sats;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_sats", nullable = false)
    private TypeSats typeSats;

    @ManyToOne(optional = false)
    @JoinColumn(name = "oppdrag110_id", nullable = false)
    private Oppdrag110 oppdrag110;

    @Embedded
    private Utbetalingsgrad utbetalingsgrad;

    @Enumerated(EnumType.STRING)
    @Column(name = "kode_status_linje")
    private KodeStatusLinje kodeStatusLinje;

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

    protected Oppdragslinje150() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KodeEndringLinje getKodeEndringLinje() {
        return kodeEndringLinje;
    }

    public KodeStatusLinje getKodeStatusLinje() {
        return kodeStatusLinje;
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

    public KodeKlassifik getKodeKlassifik() {
        return kodeKlassifik;
    }

    // Kun testformål for å sjekke ett tilfelle av migrering
    public void setKodeKlassifik(KodeKlassifik kodeKlassifik) {
        this.kodeKlassifik = kodeKlassifik;
    }

    public LocalDate getDatoVedtakFom() {
        return vedtakPeriode.getFomDato();
    }

    public LocalDate getDatoVedtakTom() {
        return vedtakPeriode.getTomDato();
    }

    public Sats getSats() {
        return sats;
    }

    public TypeSats getTypeSats() {
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

    public boolean gjelderOpphør() {
        return getKodeStatusLinje() != null;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Oppdragslinje150 oppdrlinje150)) {
            return false;
        }
        return Objects.equals(kodeEndringLinje, oppdrlinje150.getKodeEndringLinje()) && Objects.equals(kodeStatusLinje,
            oppdrlinje150.getKodeStatusLinje()) && Objects.equals(datoStatusFom, oppdrlinje150.getDatoStatusFom()) && Objects.equals(vedtakId,
            oppdrlinje150.getVedtakId()) && Objects.equals(delytelseId, oppdrlinje150.getDelytelseId()) && Objects.equals(kodeKlassifik,
            oppdrlinje150.getKodeKlassifik()) && Objects.equals(vedtakPeriode, oppdrlinje150.vedtakPeriode) && Objects.equals(sats,
            oppdrlinje150.getSats()) && Objects.equals(typeSats, oppdrlinje150.getTypeSats()) && Objects.equals(utbetalesTilId,
            oppdrlinje150.getUtbetalesTilId()) && Objects.equals(refFagsystemId, oppdrlinje150.getRefFagsystemId()) && Objects.equals(refDelytelseId,
            oppdrlinje150.getRefDelytelseId()) && Objects.equals(utbetalingsgrad, oppdrlinje150.getUtbetalingsgrad());
    }

    @Override
    public int hashCode() {
        return Objects.hash(kodeEndringLinje, kodeStatusLinje, datoStatusFom, vedtakId, delytelseId, kodeKlassifik, vedtakPeriode, sats, typeSats,
            utbetalesTilId, refFagsystemId, refDelytelseId, utbetalingsgrad);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "kodeEndringLinje=" + kodeEndringLinje + ", "
            + "kodeStatusLinje=" + kodeStatusLinje + ", "
            + "datoStatusFom=" + datoStatusFom + ", "
            + "vedtakId=" + vedtakId + ", "
            + "delytelseId=" + delytelseId + ", "
            + "kodeKlassifik=" + kodeKlassifik + ", "
            + "vedtakPeriode=" + vedtakPeriode + ", "
            + "sats=" + sats + ", "
            + "typeSats=" + typeSats + ", "
            + "utbetalesTilId=" + utbetalesTilId + ", "
            + "refFagsystemId=" + refFagsystemId + ", "
            + "refDelytelseId=" + refDelytelseId + ", "
            + "grad=" + utbetalingsgrad + ", "
            + "opprettetTs=" + getOpprettetTidspunkt()
            + ">";
    }

    public static class Builder {
        private KodeEndringLinje kodeEndringLinje;
        private KodeStatusLinje kodeStatusLinje;
        private LocalDate datoStatusFom;
        private String vedtakId;
        private Long delytelseId;
        private KodeKlassifik kodeKlassifik;
        private DatoIntervallEntitet vedtakPeriode;
        private Sats sats;
        private TypeSats typeSats;
        private String utbetalesTilId;
        private Long refFagsystemId;
        private Long refDelytelseId;
        private Oppdrag110 oppdrag110;
        private Utbetalingsgrad utbetalingsgrad;

        public Builder medKodeEndringLinje(KodeEndringLinje kodeEndringLinje) {
            this.kodeEndringLinje = kodeEndringLinje;
            return this;
        }

        public Builder medKodeStatusLinje(KodeStatusLinje kodeStatusLinje) {
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

        public Builder medKodeKlassifik(KodeKlassifik kodeKlassifik) {
            this.kodeKlassifik = kodeKlassifik;
            return this;
        }

        public Builder medVedtakFomOgTom(LocalDate datoVedtakFom, LocalDate datoVedtakTom) {
            this.vedtakPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(datoVedtakFom, datoVedtakTom);
            return this;
        }

        public Builder medSats(Sats sats) {
            this.sats = sats;
            return this;
        }

        public Builder medTypeSats(TypeSats typeSats) {
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
            var oppdragslinje150 = new Oppdragslinje150();
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
