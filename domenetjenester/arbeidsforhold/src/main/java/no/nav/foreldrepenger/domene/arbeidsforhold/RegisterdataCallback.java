package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegisterdataCallback(Long behandlingId, UUID eksisterendeGrunnlagRef, UUID oppdatertGrunnlagRef, LocalDateTime oppdatertTidspunkt) {

}
