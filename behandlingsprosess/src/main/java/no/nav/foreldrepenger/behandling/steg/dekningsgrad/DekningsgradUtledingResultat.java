package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;

record DekningsgradUtledingResultat(Dekningsgrad dekningsgrad, DekningsgradKilde kilde) {

    enum DekningsgradKilde {
        FAGSAK_RELASJON, DØDSFALL, ALLEREDE_FASTSATT, OPPGITT
    }
}
