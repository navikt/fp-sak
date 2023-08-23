package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "BehandlingVedtak")
@Table(name = "BEHANDLING_VEDTAK")
public class BehandlingVedtak extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_VEDTAK")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "VEDTAK_DATO", nullable = false)
    private LocalDateTime vedtakstidspunkt;

    @Column(name = "ANSVARLIG_SAKSBEHANDLER", nullable = false)
    private String ansvarligSaksbehandler;

    @Convert(converter = VedtakResultatType.KodeverdiConverter.class)
    @Column(name = "vedtak_resultat_type", nullable = false)
    private VedtakResultatType vedtakResultatType = VedtakResultatType.UDEFINERT;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BEHANDLING_RESULTAT_ID", nullable = false, updatable = false, unique = true)
    private Behandlingsresultat behandlingsresultat;

    /**
     * Hvorvidt vedtaket er et "beslutningsvedtak". Et beslutningsvedtak er et
     * vedtak med samme utfall som forrige vedtak.
     *
     * @see https://jira.adeo.no/browse/BEGREP-2012
     */
    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "BESLUTNING", nullable = false)
    private boolean beslutningsvedtak;

    @Convert(converter = IverksettingStatus.KodeverdiConverter.class)
    @Column(name = "iverksetting_status", nullable = false)
    private IverksettingStatus iverksettingStatus = IverksettingStatus.UDEFINERT;

    private BehandlingVedtak() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getVedtaksdato() {
        return vedtakstidspunkt.toLocalDate();
    }

    public LocalDateTime getVedtakstidspunkt() {
        return vedtakstidspunkt;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public VedtakResultatType getVedtakResultatType() {
        return Objects.equals(VedtakResultatType.UDEFINERT, vedtakResultatType) ? null : vedtakResultatType;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public Boolean isBeslutningsvedtak() {
        return beslutningsvedtak;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof BehandlingVedtak vedtak)) {
            return false;
        }
        return Objects.equals(vedtakstidspunkt, vedtak.getVedtakstidspunkt())
                && Objects.equals(ansvarligSaksbehandler, vedtak.getAnsvarligSaksbehandler())
                && Objects.equals(getVedtakResultatType(), vedtak.getVedtakResultatType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(vedtakstidspunkt, ansvarligSaksbehandler, getVedtakResultatType());
    }

    public static Builder builder() {
        return new Builder();
    }

    public IverksettingStatus getIverksettingStatus() {
        return iverksettingStatus;
    }

    public void setIverksettingStatus(IverksettingStatus iverksettingStatus) {
        this.iverksettingStatus = iverksettingStatus == null ? IverksettingStatus.UDEFINERT : iverksettingStatus;
    }

    public static class Builder {
        private LocalDateTime vedtakstidspunkt;
        private String ansvarligSaksbehandler;
        private VedtakResultatType vedtakResultatType;
        private Behandlingsresultat behandlingsresultat;
        private IverksettingStatus iverksettingStatus = IverksettingStatus.IKKE_IVERKSATT;
        private boolean beslutning = false;

        public Builder medVedtakstidspunkt(LocalDateTime vedtakstidspunkt) {
            this.vedtakstidspunkt = vedtakstidspunkt;
            return this;
        }

        public Builder medAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
            this.ansvarligSaksbehandler = ansvarligSaksbehandler;
            return this;
        }

        public Builder medVedtakResultatType(VedtakResultatType vedtakResultatType) {
            this.vedtakResultatType = vedtakResultatType;
            return this;
        }

        public Builder medBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
            this.behandlingsresultat = behandlingsresultat;
            return this;
        }

        public Builder medIverksettingStatus(IverksettingStatus iverksettingStatus) {
            this.iverksettingStatus = iverksettingStatus;
            return this;
        }

        public Builder medBeslutning(boolean beslutning) {
            this.beslutning = beslutning;
            return this;
        }

        public BehandlingVedtak build() {
            verifyStateForBuild();
            var vedtak = new BehandlingVedtak();
            vedtak.vedtakstidspunkt = vedtakstidspunkt;
            vedtak.ansvarligSaksbehandler = ansvarligSaksbehandler;
            vedtak.vedtakResultatType = vedtakResultatType;
            vedtak.behandlingsresultat = behandlingsresultat;
            vedtak.beslutningsvedtak = beslutning;
            vedtak.setIverksettingStatus(iverksettingStatus);
            return vedtak;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(vedtakstidspunkt, "vedtakstidspunkt");
            Objects.requireNonNull(ansvarligSaksbehandler, "ansvarligSaksbehandler");
            Objects.requireNonNull(vedtakResultatType, "vedtakResultatType");
        }
    }

    public void setId(long vedtakId) {
        this.id = vedtakId;

    }

}
