package no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;


public record NøkkeltallBehandlingVentefristUtløper(String behandlendeEnhet, FagsakYtelseType fagsakYtelseType,
                                                    String fristUke, Long antall) {

}
