package no.nav.foreldrepenger.kompletthet;

import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

public interface Kompletthetsjekker {
    KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref);

    KompletthetResultat vurderSøknadMottattForTidlig(Skjæringstidspunkt stp);

    KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp);

    List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref);

    List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref);

    boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref);

    @SuppressWarnings("unused")
    default KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return KompletthetResultat.oppfylt();
    }
}
