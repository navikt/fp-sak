package no.nav.foreldrepenger.behandling.revurdering;

import no.nav.vedtak.exception.TekniskException;

public final class RevurderingFeil {

    private RevurderingFeil() {
    }

    public static TekniskException tjenesteFinnerIkkeBehandlingForRevurdering(Long fagsakId) {
        return new TekniskException("FP-317517", String.format("finner ingen behandling som kan revurderes for fagsak: %s", fagsakId));
    }

    public static TekniskException revurderingManglerOriginalBehandling(Long behandlingId) {
        return new TekniskException("FP-186234", String.format("Revurdering med id %s har ikke original behandling", behandlingId));
    }
}
