package no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;


public record NøkkeltallBehandlingVentefristUtløper(String behandlendeEnhet, FagsakYtelseType fagsakYtelseType,
                                                    LocalDate behandlingFrist, String fristUke, Long antall) {

}
