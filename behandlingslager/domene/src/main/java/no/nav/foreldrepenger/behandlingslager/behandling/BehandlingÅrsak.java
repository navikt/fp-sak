package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;

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
            for (BehandlingÅrsakType årsakType : this.behandlingÅrsakTyper) {
                // Tillater å oppdatere enkelte attributter. Kan derfor ikke bruke Hibernate + equals/hashcode til å håndtere insert vs update
                Optional<BehandlingÅrsak> eksisterende = behandling.getBehandlingÅrsaker().stream()
                    .filter(it -> it.getBehandlingÅrsakType().equals(årsakType))
                    .findFirst();
                if (eksisterende.isPresent()) {
                    // Oppdater eksisterende (UPDATE)
                    BehandlingÅrsak årsak = eksisterende.get();
                    if (this.originalBehandlingId != null) {
                        årsak.originalBehandlingId = this.originalBehandlingId;
                    }
                    årsak.manueltOpprettet = this.manueltOpprettet;
                } else {
                    // Opprett ny (INSERT)
                    BehandlingÅrsak behandlingÅrsak = new BehandlingÅrsak();
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
        BehandlingÅrsak that = (BehandlingÅrsak) o;

        return Objects.equals(behandlingÅrsakType, that.behandlingÅrsakType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingÅrsakType);
    }
}
