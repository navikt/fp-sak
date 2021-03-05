package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import no.nav.vedtak.exception.TekniskException;

public final class VilkårUtlederFeil {

    private VilkårUtlederFeil() {
    }

    public static TekniskException behandlingsmotivKanIkkeUtledes(Long behandlingId) {
        var msg = String.format("Kan ikke utlede vilkår for behandlingId %s, da behandlingsmotiv ikke kan avgjøres", behandlingId);
        throw new TekniskException("FP-768019", msg);
    }
}
