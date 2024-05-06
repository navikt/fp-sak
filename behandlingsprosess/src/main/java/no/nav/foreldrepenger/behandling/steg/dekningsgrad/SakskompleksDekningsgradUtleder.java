package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;

final class SakskompleksDekningsgradUtleder {

    private SakskompleksDekningsgradUtleder() {
    }

    static Optional<Dekningsgrad> utledFor(Dekningsgrad fagsakRelasjonDekningsgrad,
                                           Dekningsgrad eksisterendeSakskompleksDekningsgrad,
                                           Dekningsgrad oppgittDekningsgrad,
                                           Dekningsgrad annenPartsOppgittDekningsgrad,
                                           FamilieHendelseEntitet familieHendelseEntitet) {
        //TODO TFP-5702: Se oppgitt vs fagsakrel vs annen parts åpen behandling vs dødsfall. Empty for å opprette AP hvis uavklart
        return Optional.of(fagsakRelasjonDekningsgrad);
    }
}
