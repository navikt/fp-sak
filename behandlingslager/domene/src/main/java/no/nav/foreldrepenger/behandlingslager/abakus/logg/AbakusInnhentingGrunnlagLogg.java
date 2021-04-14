package no.nav.foreldrepenger.behandlingslager.abakus.logg;


import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity
@Table(name = "ABAKUS_INNHENTING_LOGG")
public class AbakusInnhentingGrunnlagLogg extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ABAKUS_INN_LOGG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "grunnlag_uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    AbakusInnhentingGrunnlagLogg() {
    }

    public AbakusInnhentingGrunnlagLogg(Long behandlingId, UUID grunnlagUuid) {
        this.behandlingId = behandlingId;
        this.uuid = grunnlagUuid;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public UUID getUuid() {
        return uuid;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (AbakusInnhentingGrunnlagLogg) o;
        return Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, uuid);
    }

    @Override
    public String toString() {
        return "AbakusInnhentingGrunnlagLogg{" +
            "id=" + id +
            ", behandlingId=" + behandlingId +
            ", uuid=" + uuid +
            '}';
    }
}
