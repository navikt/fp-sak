package no.nav.foreldrepenger.kompletthet.impl.svp;

import static no.nav.foreldrepenger.kompletthet.implV2.KompletthetsjekkerFelles.VENTEFRIST_FOR_MANGLENDE_SØKNAD;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerOld;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetssjekkerSøknad;
import no.nav.foreldrepenger.kompletthet.implV2.KompletthetsjekkerFelles;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class KompletthetsjekkerImpl implements KompletthetsjekkerOld {


    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private KompletthetsjekkerFelles fellesUtil;

    private static final Logger LOG = LoggerFactory.getLogger(KompletthetsjekkerImpl.class);

    public KompletthetsjekkerImpl() {
        //CDI
    }

    @Inject
    public KompletthetsjekkerImpl(@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) KompletthetssjekkerSøknad kompletthetssjekkerSøknad,
                                 KompletthetsjekkerFelles fellesUtil) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.fellesUtil = fellesUtil;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        if (Boolean.FALSE.equals(kompletthetssjekkerSøknad.erSøknadMottatt(ref))) {
            // Litt implisitt forutsetning her, men denne sjekken skal bare ha bli kalt dersom søknad eller IM er mottatt
            LOG.info("Behandling {} er ikke komplett - søknad er ikke mottatt", ref.behandlingId());
            return KompletthetResultat.ikkeOppfylt(finnVentefristTilManglendeSøknad(), Venteårsak.AVV_DOK);
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var forTidligFrist = kompletthetssjekkerSøknad.erSøknadMottattForTidlig(stp);
        return forTidligFrist.map(localDateTime -> KompletthetResultat.ikkeOppfylt(localDateTime, Venteårsak.FOR_TIDLIG_SOKNAD)).orElseGet(KompletthetResultat::oppfylt);
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        // Kalles fra VurderKompletthetSteg (en ganger) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        if (ref.erRevurdering()) {
            return KompletthetResultat.oppfylt();
        } else {
            var kompletthetManglendeIM = fellesUtil.getInntektsmeldingKomplett(ref, stp);
            return kompletthetManglendeIM.orElseGet(KompletthetResultat::oppfylt);
        }
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return Collections.emptyList();//Påkrevde vedlegg håndheves i søknadsdialogen
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return Collections.emptyList();//Påkrevde vedlegg håndheves i søknadsdialogen
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return true; //Håndteres manuellt
    }

    @Override
    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return ref.erRevurdering() ? KompletthetResultat.oppfylt() : fellesUtil.vurderEtterlysningInntektsmelding(ref, stp);
    }

    private LocalDateTime finnVentefristTilManglendeSøknad() {
        return LocalDateTime.now().plusWeeks(VENTEFRIST_FOR_MANGLENDE_SØKNAD);
    }
}
