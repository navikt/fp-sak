package no.nav.foreldrepenger.inngangsvilkaar.utleder;

import no.nav.vedtak.exception.TekniskException;

final class VilkårUtlederFeil {

    private VilkårUtlederFeil() {
    }

    static TekniskException behandlingsmotivKanIkkeUtledes(Long behandlingId) {
        var msg = String.format("Kan ikke utlede vilkår for behandlingId %s, da behandlingsmotiv ikke kan avgjøres",
            behandlingId);
        return new TekniskException("FP-768017", msg);
    }

    static TekniskException kunneIkkeUtledeVilkårFor(Long behandlingId, String behandlingsmotiv) {
        var msg = String.format(
            "Kan ikke utlede vilkår for behandlingId %s. Mangler konfigurasjon for behandlingsmotiv %s", behandlingId,
            behandlingsmotiv);
        return new TekniskException("FP-768018", msg);
    }

}
