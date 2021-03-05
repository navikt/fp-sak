package no.nav.foreldrepenger.behandling.steg.varselrevurdering;

import no.nav.vedtak.exception.TekniskException;

public final class VarselRevurderingStegFeil {

    private VarselRevurderingStegFeil() {
    }

    public static TekniskException manglerBehandlingsårsakPåRevurdering() {
        throw new TekniskException("FP-139371", "Manger behandlingsårsak på revurdering");
    }
}
