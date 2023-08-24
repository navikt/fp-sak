package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerSøknad;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetsjekkerFelles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetsjekkerFelles.VENTEFRIST_FOR_MANGLENDE_SØKNAD;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class KompletthetsjekkerImpl implements Kompletthetsjekker {


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
        if (!kompletthetssjekkerSøknad.erSøknadMottatt(ref)) {
            // Litt implisitt forutsetning her, men denne sjekken skal bare ha bli kalt dersom søknad eller IM er mottatt
            LOG.info("Behandling {} er ikke komplett - søknad er ikke mottatt", ref.behandlingId());
            return KompletthetResultat.ikkeOppfylt(finnVentefristTilManglendeSøknad(), Venteårsak.AVV_DOK);
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        var forTidligFrist = kompletthetssjekkerSøknad.erSøknadMottattForTidlig(ref);
        return forTidligFrist.map(localDateTime -> KompletthetResultat.ikkeOppfylt(localDateTime, Venteårsak.FOR_TIDLIG_SOKNAD)).orElseGet(KompletthetResultat::oppfylt);
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        // Kalles fra VurderKompletthetSteg (en ganger) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        var kompletthetManglendeIM = fellesUtil.getInntektsmeldingKomplett(ref);
        return kompletthetManglendeIM.orElseGet(KompletthetResultat::oppfylt);
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
        return true;//Håndteres manuellt
    }

    @Override
    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref) {
        return fellesUtil.vurderEtterlysningInntektsmelding(ref);
    }

    private LocalDateTime finnVentefristTilManglendeSøknad() {
        return LocalDateTime.now().plusWeeks(VENTEFRIST_FOR_MANGLENDE_SØKNAD);
    }
}
