package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerInntektsmelding;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerSøknad;

@ApplicationScoped
@BehandlingTypeRef("BT-002")
@FagsakYtelseTypeRef("FP")
public class KompletthetsjekkerImpl implements Kompletthetsjekker {
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetsjekkerImpl.class);


    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding;
    private KompletthetsjekkerFelles fellesUtil;

    KompletthetsjekkerImpl() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerImpl(@FagsakYtelseTypeRef("FP") @BehandlingTypeRef("BT-002") KompletthetssjekkerSøknad kompletthetssjekkerSøknad,
                                KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding,
                                KompletthetsjekkerFelles fellesUtil) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.kompletthetssjekkerInntektsmelding = kompletthetssjekkerInntektsmelding;
        this.fellesUtil = fellesUtil;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        if (!kompletthetssjekkerSøknad.erSøknadMottatt(ref)) {
            // Litt implisitt forutsetning her, men denne sjekken skal bare ha bli kalt dersom søknad eller IM er mottatt
            LOG.info("Behandling {} er ikke komplett - søknad er ikke mottatt", ref.getBehandlingId()); // NOSONAR //$NON-NLS-1$
            return KompletthetResultat.ikkeOppfylt(fellesUtil.finnVentefristTilManglendeSøknad(), Venteårsak.AVV_DOK);
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        var forTidligFrist = kompletthetssjekkerSøknad.erSøknadMottattForTidlig(ref);
        if (forTidligFrist.isPresent()) {
            return KompletthetResultat.ikkeOppfylt(forTidligFrist.get(), Venteårsak.FOR_TIDLIG_SOKNAD);
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        var behandlingId = ref.getBehandlingId();
        if (BehandlingStatus.OPPRETTET.equals(ref.getBehandlingStatus())) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra VurderKompletthetSteg (en gang) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        var kompletthetManglendeIM = fellesUtil.getInntektsmeldingKomplett(ref);
        if (kompletthetManglendeIM.isPresent()) {
            return kompletthetManglendeIM.get();
        }
        // Denne fristen skulle egentlig vært samordnet med frist over - men man ønsket få opp IM-mangler uavhengig
        if (!kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref).isEmpty()) {
            var ventefristTidligMottattSøknad = fellesUtil.finnVentefristTilForTidligMottattSøknad(behandlingId);
            return ventefristTidligMottattSøknad
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.fristUtløpt());
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        var manglendeVedlegg = kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref);
        manglendeVedlegg.addAll(kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldingerFraGrunnlag(ref));
        return manglendeVedlegg.isEmpty();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        var manglendeVedlegg = kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref);
        manglendeVedlegg.addAll(kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldingerFraGrunnlag(ref));
        return manglendeVedlegg;
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return fellesUtil.hentAlleInntektsmeldingerSomIkkeKommer(ref)
            .stream()
            .map(e -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, e.getArbeidsgiver().getIdentifikator(), true))
            .collect(Collectors.toList());
    }

    @Override
    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref) {
        return fellesUtil.vurderEtterlysningInntektsmelding(ref);
    }

}
