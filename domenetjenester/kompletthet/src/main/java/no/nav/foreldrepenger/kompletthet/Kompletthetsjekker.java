package no.nav.foreldrepenger.kompletthet;

import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

public interface Kompletthetsjekker {
    KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref);

    KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref, Skjæringstidspunkt stp);

    KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp);

    List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref);

    List<ManglendeVedlegg> utledAlleManglendeInntektsmeldingerSomIkkeKommer(BehandlingReferanse ref);

    KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp);
}
