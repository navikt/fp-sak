package no.nav.foreldrepenger.behandlingslager.lagretvedtak;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class LagretVedtakMedBehandlingType {

    // Id er behandlingId på lagret vedtak
    private final Long id;
    private final UUID uuid;
    private final String behandlingType;
    private final LocalDate opprettetDato;

    public LagretVedtakMedBehandlingType(BigDecimal id, UUID uuid, String behandlingType, Object opprettetDato) {
        this.id = id.longValue();
        this.uuid = uuid;
        this.behandlingType = behandlingType;
        this.opprettetDato =    ((Timestamp)opprettetDato).toLocalDateTime().toLocalDate();
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getBehandlingType() {
        return behandlingType;
    }

    public LocalDate getOpprettetDato() {
        return opprettetDato;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LagretVedtakMedBehandlingType)) {
            return false;
        }

        var that = (LagretVedtakMedBehandlingType) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uuid, behandlingType, opprettetDato);
    }


    @Override
    public String toString() {
        return "LagretVedtakMedBehandlingType{" + "id=" + id + ", uuid=" + uuid + ", behandlingType='" + behandlingType + '\'' + ", opprettetDato="
            + opprettetDato + '}';
    }
}
