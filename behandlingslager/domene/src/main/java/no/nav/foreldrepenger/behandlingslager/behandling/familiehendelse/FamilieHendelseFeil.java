package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import no.nav.vedtak.exception.TekniskException;

final class FamilieHendelseFeil {
    private FamilieHendelseFeil() {
    }

    static TekniskException fantIkkeForventetGrunnlagPåBehandling(long behandlingId) {
        return new TekniskException("FP-947392", "Finner ikke FamilieHendelse grunnlag for behandling med id " + behandlingId);
    }

    static TekniskException kanIkkeEndreTypePåHendelseFraTil(FamilieHendelseType fraType, FamilieHendelseType tilType) {
        var msg = String.format("Kan ikke endre typen til '%s' når den er '%s'", fraType, tilType);
        return new TekniskException("FP-903132", msg);
    }

    static TekniskException måBasereSegPåEksisterendeVersjon() {
        return new TekniskException("FP-124902", "Må basere seg på eksisterende versjon av familiehendelsen");
    }

    static TekniskException kanIkkeOppdatereSøknadVersjon() {
        return new TekniskException("FP-947231", "Kan ikke oppdatere søknadsversjonen etter at det har blitt satt.");
    }

    static TekniskException aggregatKanIkkeVæreNull() {
        return new TekniskException("FP-949165", "Aggregat kan ikke være null ved opprettelse av builder");
    }

    static TekniskException ukjentVersjonstype() {
        return new TekniskException("FP-536282", "Ukjent versjonstype ved opprettelse av builder");
    }

}
