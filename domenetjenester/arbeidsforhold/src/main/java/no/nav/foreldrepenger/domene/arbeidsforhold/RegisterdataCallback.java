package no.nav.foreldrepenger.domene.arbeidsforhold;
import java.time.LocalDateTime;
import java.util.UUID;

public class RegisterdataCallback {

    private final Long behandlingId;
    private final UUID eksisterendeGrunnlagRef;
    private final UUID oppdatertGrunnlagRef;
    private final LocalDateTime oppdatertTidspunkt;

    public RegisterdataCallback(Long behandlingId, UUID eksisterendeGrunnlagRef, UUID oppdatertGrunnlagRef, LocalDateTime oppdatertTidspunkt) {
        this.behandlingId = behandlingId;
        this.eksisterendeGrunnlagRef = eksisterendeGrunnlagRef;
        this.oppdatertGrunnlagRef = oppdatertGrunnlagRef;
        this.oppdatertTidspunkt = oppdatertTidspunkt;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public UUID getEksisterendeGrunnlagRef() {
        return eksisterendeGrunnlagRef;
    }

    public UUID getOppdatertGrunnlagRef() {
        return oppdatertGrunnlagRef;
    }

    public LocalDateTime getOppdatertTidspunkt() {
        return oppdatertTidspunkt;
    }

    @Override
    public String toString() {
        return "RegisterdataCallback{" +
            "behandlingId=" + behandlingId +
            ", eksisterendeGrunnlagRef=" + eksisterendeGrunnlagRef +
            ", oppdatertGrunnlagRef=" + oppdatertGrunnlagRef +
            ", oppdatertTidspunkt=" + oppdatertTidspunkt +
            '}';
    }
}