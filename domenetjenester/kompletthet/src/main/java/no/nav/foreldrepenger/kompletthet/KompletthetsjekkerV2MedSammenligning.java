package no.nav.foreldrepenger.kompletthet;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerOld;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.kompletthet.implV2.KompletthetsjekkerTjeneste;

@ApplicationScoped
public class KompletthetsjekkerV2MedSammenligning implements Kompletthetsjekker {
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetsjekkerV2MedSammenligning.class);

    private KompletthetsjekkerTjeneste kompletthetsjekkerTjeneste;
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;

    public KompletthetsjekkerV2MedSammenligning() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerV2MedSammenligning(KompletthetsjekkerTjeneste kompletthetsjekkerTjeneste,
                                                KompletthetsjekkerProvider kompletthetsjekkerProvider) {
        this.kompletthetsjekkerTjeneste = kompletthetsjekkerTjeneste;
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        var resultat = finnKompletthetssjekker(ref).vurderSøknadMottatt(ref);
        sammenlignResultatFraNyKompletthet(ref, resultat, "vurderSøknadMottatt", kompletthetsjekkerTjeneste::vurderSøknadMottatt);
        return resultat;
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var resultat = finnKompletthetssjekker(ref).vurderSøknadMottattForTidlig(ref, stp);
        sammenlignResultatFraNyKompletthet(ref, stp, resultat, "vurderSøknadMottattForTidlig", kompletthetsjekkerTjeneste::vurderSøknadMottattForTidlig);
        return resultat;
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var resultat = finnKompletthetssjekker(ref).vurderForsendelseKomplett(ref, stp);
        sammenlignResultatFraNyKompletthet(ref, stp, resultat, "vurderForsendelseKomplett", kompletthetsjekkerTjeneste::vurderForsendelseKomplett);
        return resultat;
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        var resultat = finnKompletthetssjekker(ref).utledAlleManglendeVedleggForForsendelse(ref);
        sammenlignResultatFraNyKompletthet(ref, resultat, "utledAlleManglendeVedleggForForsendelse", kompletthetsjekkerTjeneste::utledAlleManglendeVedleggForForsendelse);
        return resultat;
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        var resultat = finnKompletthetssjekker(ref).utledAlleManglendeVedleggSomIkkeKommer(ref);
        sammenlignResultatFraNyKompletthet(ref, resultat, "utledAlleManglendeVedleggSomIkkeKommer", kompletthetsjekkerTjeneste::utledAlleManglendeVedleggSomIkkeKommer);
        return resultat;
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        var resultat = finnKompletthetssjekker(ref).erForsendelsesgrunnlagKomplett(ref);
        sammenlignResultatFraNyKompletthet(ref, resultat, "erForsendelsesgrunnlagKomplett", kompletthetsjekkerTjeneste::erForsendelsesgrunnlagKomplett);
        return resultat;
    }

    @Override
    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var resultat = finnKompletthetssjekker(ref).vurderEtterlysningInntektsmelding(ref, stp);
        sammenlignResultatFraNyKompletthet(ref, stp, resultat, "vurderEtterlysningInntektsmelding", kompletthetsjekkerTjeneste::vurderEtterlysningInntektsmelding);
        return resultat;
    }


    private KompletthetsjekkerOld finnKompletthetssjekker(BehandlingReferanse ref) {
        return kompletthetsjekkerProvider.finnKompletthetsjekkerFor(ref.fagsakYtelseType(), ref.behandlingType());
    }

    private <T> void sammenlignResultatFraNyKompletthet(BehandlingReferanse ref, T resultat, String metodeNavn,
                                                        Function<BehandlingReferanse, T> metode) {
        try {
            var resultatNy = metode.apply(ref);
            if (!Objects.equals(resultat, resultatNy)) {
                LOG.info("KompletthetV2: Ulikt resultat for {}(). GAMMEL: {}, NY: {}", metodeNavn, resultat, resultatNy);
            } else {
                LOG.info("KompletthetV2: Likt resultat for {}()", metodeNavn);
            }
        } catch (Exception e) {
            LOG.error("KompletthetV2: Sammenligning feilet {}()", metodeNavn, e);
        }
    }

    private <T> void sammenlignResultatFraNyKompletthet(BehandlingReferanse ref, Skjæringstidspunkt stp, T resultat, String metodeNavn,
                                                        BiFunction<BehandlingReferanse, Skjæringstidspunkt, T> metode) {
        try {
            var resultatNy = metode.apply(ref, stp);
            if (!Objects.equals(resultat, resultatNy)) {
                LOG.info("KompletthetV2: Ulikt resultat for {}. GAMMEL: {}, NY: {}", metodeNavn, resultat, resultatNy);
            } else {
                LOG.info("KompletthetV2: Likt resultat for {}", metodeNavn);
            }
        } catch (Exception e) {
            LOG.error("KompletthetV2: Sammenligning feilet {}", metodeNavn, e);
        }
    }
}
