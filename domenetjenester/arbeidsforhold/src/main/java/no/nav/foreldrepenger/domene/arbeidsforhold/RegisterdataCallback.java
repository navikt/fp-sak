package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.util.UUID;

public record RegisterdataCallback(Long behandlingId, UUID eksisterendeGrunnlagRef, UUID oppdatertGrunnlagRef) {

}
