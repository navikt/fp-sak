package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

class FagsakRelasjonBuilder {

    private FagsakYtelseType ytelseType;

    FagsakRelasjonBuilder(FagsakYtelseType type) {
        ytelseType = type;
    }

    static FagsakRelasjonBuilder engangsstønad() {
        return new FagsakRelasjonBuilder(FagsakYtelseType.ENGANGSTØNAD);
    }

    static FagsakRelasjonBuilder foreldrepenger() {
        return new FagsakRelasjonBuilder(FagsakYtelseType.FORELDREPENGER);
    }

    FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

}
