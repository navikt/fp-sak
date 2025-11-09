package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@Entity(name = "OverlappVedtak")
@Table(name = "OVERLAPP_VEDTAK")
public class OverlappVedtak extends BaseEntitet {

    // Konvensjon VEDTAK-<TEMA> + evt ytelse og saksnummer/referanse
    public static final String HENDELSE_VEDTAK_FOR = "VEDTAK-FOR";
    public static final String HENDELSE_VEDTAK_OMS = "VEDTAK-OMS";
    public static final String HENDELSE_VEDTAK_SYK = "VEDTAK-SYK";

    // Konvensjon AVTEMMING-<formål>-<sak/periode-ref>
    public static final String HENDELSE_AVSTEM_PERIODE = "AVSTEM-PER";
    public static final String HENDELSE_AVSTEM_SAK = "AVSTEM-SAK";


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OVERLAPP_VEDTAK")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Embedded
    @AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer", nullable = false))
    private Saksnummer saksnummer;

    @Column(name = "BEHANDLING_ID", nullable = false, updatable = false)
    private Long behandlingId;

    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "FOM", nullable = false))
    @AttributeOverride(name = "tomDato", column = @Column(name = "TOM", nullable = false))
    private ÅpenDatoIntervallEntitet periode;

    @Column(name = "HENDELSE", nullable = false)
    private String hendelse;

    @Convert(converter = Fagsystem.KodeverdiConverter.class)
    @Column(name = "FAGSYSTEM", nullable = false)
    private Fagsystem fagsystem;

    @Column(name = "YTELSE", nullable = false)
    private String ytelse;

    @Column(name = "REFERANSE")
    private String referanse;

    @Column(name = "UTBETALINGSPROSENT", nullable = false)
    private long utbetalingsprosent;
    @Transient
    private long fpsakUtbetalingsprosent;


    protected OverlappVedtak() {
    }

    public Long getId() {
        return id;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public ÅpenDatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getHendelse() {
        return hendelse;
    }

    public Fagsystem getFagsystem() {
        return fagsystem;
    }

    public OverlappYtelseType getYtelse() {
        return switch (ytelse) {
            case "PSB", "PPN" -> OverlappYtelseType.PLEIEPENGER;
            case "OMP" -> OverlappYtelseType.OMSORGSPENGER;
            case "OLP" -> OverlappYtelseType.OPPLÆRINGSPENGER;
            default -> OverlappYtelseType.valueOf(ytelse);
        };
    }

    public String getReferanse() {
        return referanse;
    }

    public long getUtbetalingsprosent() {
        return utbetalingsprosent;
    }
    public long getFpsakUtbetalingsprosent() {
        return fpsakUtbetalingsprosent;
    }

    public enum OverlappYtelseType { SP, BS, PLEIEPENGER, OMSORGSPENGER, OPPLÆRINGSPENGER, FRISINN }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (OverlappVedtak) o;
        return versjon == that.versjon &&
            utbetalingsprosent == that.utbetalingsprosent &&
            Objects.equals(saksnummer, that.saksnummer) &&
            Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(periode, that.periode) &&
            Objects.equals(hendelse, that.hendelse) &&
            Objects.equals(fagsystem, that.fagsystem) &&
            Objects.equals(ytelse, that.ytelse) &&
            Objects.equals(referanse, that.referanse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versjon, saksnummer, behandlingId, periode, hendelse, fagsystem, ytelse, referanse, utbetalingsprosent);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OverlappVedtak kladd;

        private Builder() {
            kladd = new OverlappVedtak();
        }

        public Builder medSaksnummer(Saksnummer saksnummer) {
            this.kladd.saksnummer = saksnummer;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medPeriode(ÅpenDatoIntervallEntitet periode) {
            this.kladd.periode = periode;
            return this;
        }

        public Builder medHendelse(String hendelse) {
            this.kladd.hendelse = hendelse;
            return this;
        }

        public Builder medFagsystem(Fagsystem fagsystem) {
            this.kladd.fagsystem = fagsystem;
            return this;
        }

        public Builder medYtelse(OverlappYtelseType ytelse) {
            this.kladd.ytelse = ytelse.name();
            return this;
        }

        public Builder medReferanse(String referanse) {
            this.kladd.referanse = referanse;
            return this;
        }

        public Builder medUtbetalingsprosent(Long utbetalingsprosent) {
            this.kladd.utbetalingsprosent = Optional.ofNullable(utbetalingsprosent).orElse(0L);
            return this;
        }
        public Builder medFpsakUtbetalingsprosent(Long fpsakUtbetalingsprosent) {
            this.kladd.fpsakUtbetalingsprosent = Optional.ofNullable(fpsakUtbetalingsprosent).orElse(0L);
            return this;
        }

        public OverlappVedtak build() {
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.saksnummer);
            Objects.requireNonNull(kladd.behandlingId);
            Objects.requireNonNull(kladd.periode);
            Objects.requireNonNull(kladd.hendelse);
            Objects.requireNonNull(kladd.fagsystem);
            Objects.requireNonNull(kladd.ytelse);
        }
    }
}
