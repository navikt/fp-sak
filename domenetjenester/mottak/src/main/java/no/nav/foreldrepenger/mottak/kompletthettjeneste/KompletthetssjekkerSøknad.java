package no.nav.foreldrepenger.mottak.kompletthettjeneste;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

public interface KompletthetssjekkerSøknad {

    List<ManglendeVedlegg> utledManglendeVedleggForSøknad(BehandlingReferanse ref);

    Optional<LocalDateTime> erSøknadMottattForTidlig(BehandlingReferanse ref);

    Boolean erSøknadMottatt(BehandlingReferanse ref);
}
