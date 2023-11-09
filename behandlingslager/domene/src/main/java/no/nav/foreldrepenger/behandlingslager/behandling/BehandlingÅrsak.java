package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "BehandlingÅrsak")
@Table(name = "BEHANDLING_ARSAK")
public class BehandlingÅrsak extends BaseEntitet {

    @Id
    @SequenceGenerator(name = "behandling_aarsak_sekvens", sequenceName = "SEQ_BEHANDLING_ARSAK")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_aarsak_sekvens")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Convert(converter = BehandlingÅrsakType.KodeverdiConverter.class)
    @Column(name="behandling_arsak_type", nullable = false)
    private BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.UDEFINERT;

    @Column(name = "original_behandling_id", updatable = false)
    private Long originalBehandlingId;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "manuelt_opprettet", nullable = false)
    private boolean manueltOpprettet = false;

    BehandlingÅrsak() {
        // for hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandling.getId();
    }

    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return behandlingÅrsakType;
    }

    public Long getOriginalBehandlingId() {
        return originalBehandlingId;
    }

    public boolean erManueltOpprettet() {
        return manueltOpprettet;
    }

    public static BehandlingÅrsak.Builder builder(BehandlingÅrsakType behandlingÅrsakType) {
        return new Builder(Collections.singletonList(behandlingÅrsakType));
    }

    public static BehandlingÅrsak.Builder builder(List<BehandlingÅrsakType> behandlingÅrsakTyper) {
        return new Builder(behandlingÅrsakTyper);
    }

    void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public static class Builder {

        private List<BehandlingÅrsakType> behandlingÅrsakTyper;
        private Long originalBehandlingId;
        private boolean manueltOpprettet;

        public Builder(List<BehandlingÅrsakType> behandlingÅrsakTyper) {
            Objects.requireNonNull(behandlingÅrsakTyper, "behandlingÅrsakTyper");
            if (behandlingÅrsakTyper.isEmpty()) throw new IllegalArgumentException("Mangler behandlingÅrsakTyper");
            this.behandlingÅrsakTyper = behandlingÅrsakTyper;
        }

        public Builder medOriginalBehandlingId(Long originalBehandlingId) {
            this.originalBehandlingId = originalBehandlingId;
            return this;
        }

        public Builder medManueltOpprettet(boolean manueltOpprettet) {
            this.manueltOpprettet = manueltOpprettet;
            return this;
        }

        public List<BehandlingÅrsak> buildFor(Behandling behandling) {
            Objects.requireNonNull(behandling, "behandling");
            List<BehandlingÅrsak> nyeÅrsaker = new ArrayList<>();
            for (var årsakType : this.behandlingÅrsakTyper) {
                // Tillater å oppdatere enkelte attributter. Kan derfor ikke bruke Hibernate + equals/hashcode til å håndtere insert vs update
                var eksisterende = behandling.getBehandlingÅrsaker().stream()
                    .filter(it -> it.getBehandlingÅrsakType().equals(årsakType))
                    .findFirst();
                if (eksisterende.isPresent()) {
                    // Oppdater eksisterende (UPDATE)
                    var årsak = eksisterende.get();
                    if (this.originalBehandlingId != null) {
                        årsak.originalBehandlingId = this.originalBehandlingId;
                    }
                    årsak.manueltOpprettet = this.manueltOpprettet;
                } else {
                    // Opprett ny (INSERT)
                    var behandlingÅrsak = new BehandlingÅrsak();
                    behandlingÅrsak.behandling = behandling;
                    behandlingÅrsak.behandlingÅrsakType = årsakType;
                    behandlingÅrsak.originalBehandlingId = this.originalBehandlingId;
                    behandlingÅrsak.manueltOpprettet = this.manueltOpprettet;
                    nyeÅrsaker.add(behandlingÅrsak);
                }
            }
            behandling.leggTilBehandlingÅrsaker(nyeÅrsaker);
            return behandling.getBehandlingÅrsaker();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (BehandlingÅrsak) o;

        return Objects.equals(behandlingÅrsakType, that.behandlingÅrsakType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingÅrsakType);
    }
}
