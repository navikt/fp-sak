package no.nav.foreldrepenger.domene.opptjening;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

public record FerdiglignetNæring(Arbeidsgiver arbeidsgiver, String år, Long beløp) {
}
