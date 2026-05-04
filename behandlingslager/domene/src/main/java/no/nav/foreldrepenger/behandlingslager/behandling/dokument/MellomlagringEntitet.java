package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "BehandlingMellomlagring")
@Table(name = "BEHANDLING_MELLOMLAGRING")
public class MellomlagringEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MELLOMLAGRING")
    private Long id;

    @Column(name = "BEHANDLING_ID", nullable = false, updatable = false)
    private Long behandlingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, updatable = false)
    private MellomlagringType type;

    @Lob
    @Column(name = "INNHOLD", nullable = false)
    private String innhold;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "BESTILLING_LAAS", nullable = false)
    private boolean bestillingLåst;

    @Version
    @Column(name = "versjon", nullable = false)
    private Long versjon;

    protected MellomlagringEntitet() {
        // for hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public MellomlagringType getType() {
        return type;
    }

    public String getInnhold() {
        return innhold;
    }

    public void setInnhold(String innhold) {
        this.innhold = innhold;
    }

    public boolean isBestillingLåst() {
        return bestillingLåst;
    }

    public void setBestillingLåst(boolean bestillingLåst) {
        this.bestillingLåst = bestillingLåst;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (MellomlagringEntitet) o;
        return Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "behandlingId=" + behandlingId + ", "
            + "type=" + type
            + ">";
    }

    public static class Builder {
        private MellomlagringEntitet entitet;

        private Builder() {
            entitet = new MellomlagringEntitet();
        }

        public static Builder ny() {
            return new Builder();
        }

        public static Builder fraEksisterende(MellomlagringEntitet eksisterende) {
            var builder = new Builder();
            builder.entitet.id = eksisterende.id;
            builder.entitet.behandlingId = eksisterende.behandlingId;
            builder.entitet.type = eksisterende.type;
            builder.entitet.innhold = eksisterende.innhold;
            return builder;
        }

        public Builder medBehandlingId(Long behandlingId) {
            entitet.behandlingId = behandlingId;
            return this;
        }

        public Builder medType(MellomlagringType type) {
            entitet.type = type;
            return this;
        }

        public Builder medInnhold(String innhold) {
            entitet.innhold = innhold;
            return this;
        }

        public MellomlagringEntitet build() {
            Objects.requireNonNull(entitet.behandlingId, "BehandlingId må være satt");
            Objects.requireNonNull(entitet.type, "MellomlagringType må være satt");
            Objects.requireNonNull(entitet.innhold, "Innhold må være satt");
            return entitet;
        }
    }
}
