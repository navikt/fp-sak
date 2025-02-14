package no.nav.foreldrepenger.kompletthet.impl;

import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

public interface KompletthetsjekkerOld {
    KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref);

    KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref, Skjæringstidspunkt stp);

    KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp);

    List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref);

    List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref);

    boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref);

    @SuppressWarnings("unused")
    default KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return KompletthetResultat.oppfylt();
    }
}
