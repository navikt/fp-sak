package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
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
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer", nullable = false)))
    private Saksnummer saksnummer;

    @Column(name = "BEHANDLING_ID", nullable = false, updatable = false)
    private Long behandlingId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "FOM", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "TOM", nullable = false))
    })
    private ÅpenDatoIntervallEntitet periode;

    @Column(name = "HENDELSE", nullable = false)
    private String hendelse;

    @Column(name = "FAGSYSTEM", nullable = false)
    private String fagsystem;

    @Column(name = "YTELSE", nullable = false)
    private String ytelse;

    @Column(name = "REFERANSE")
    private String referanse;

    @Column(name = "UTBETALINGSPROSENT", nullable = false)
    private long utbetalingsprosent;


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

    public String getFagsystem() {
        return fagsystem;
    }

    public String getYtelse() {
        return ytelse;
    }

    public String getReferanse() {
        return referanse;
    }

    public long getUtbetalingsprosent() {
        return utbetalingsprosent;
    }

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

        public Builder medFagsystem(String fagsystem) {
            this.kladd.fagsystem = fagsystem;
            return this;
        }

        public Builder medYtelse(String ytelse) {
            this.kladd.ytelse = ytelse;
            return this;
        }

        public Builder medReferanse(String referanse) {
            this.kladd.referanse = referanse;
            return this;
        }

        public Builder medUtbetalingsprosent(Long utbetalingsprosent) {
            this.kladd.utbetalingsprosent = utbetalingsprosent;
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
