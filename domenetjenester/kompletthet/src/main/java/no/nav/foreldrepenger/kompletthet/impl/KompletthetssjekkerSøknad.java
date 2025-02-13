package no.nav.foreldrepenger.kompletthet.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

public interface KompletthetssjekkerSøknad {

    List<ManglendeVedlegg> utledManglendeVedleggForSøknad(BehandlingReferanse ref);

    Optional<LocalDateTime> erSøknadMottattForTidlig(Skjæringstidspunkt stp);

    Boolean erSøknadMottatt(BehandlingReferanse ref);
}
