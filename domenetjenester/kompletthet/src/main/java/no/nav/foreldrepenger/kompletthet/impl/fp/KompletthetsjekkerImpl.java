package no.nav.foreldrepenger.kompletthet.impl.fp;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerOld;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetssjekkerSøknad;
import no.nav.foreldrepenger.kompletthet.implV2.KompletthetsjekkerFelles;

@ApplicationScoped
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class KompletthetsjekkerImpl implements KompletthetsjekkerOld {
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetsjekkerImpl.class);


    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private KompletthetsjekkerFelles fellesUtil;

    KompletthetsjekkerImpl() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerImpl(@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) @BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD) KompletthetssjekkerSøknad kompletthetssjekkerSøknad,
                                  KompletthetsjekkerFelles fellesUtil) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.fellesUtil = fellesUtil;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        if (!kompletthetssjekkerSøknad.erSøknadMottatt(ref)) {
            // Litt implisitt forutsetning her, men denne sjekken skal bare ha bli kalt dersom søknad eller IM er mottatt
            LOG.info("Behandling {} er ikke komplett - søknad er ikke mottatt", ref.behandlingId());
            return KompletthetResultat.ikkeOppfylt(fellesUtil.finnVentefristTilManglendeSøknad(), Venteårsak.AVV_DOK);
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var forTidligFrist = kompletthetssjekkerSøknad.erSøknadMottattForTidlig(stp);
        if (forTidligFrist.isPresent()) {
            return KompletthetResultat.ikkeOppfylt(forTidligFrist.get(), Venteårsak.FOR_TIDLIG_SOKNAD);
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var behandlingId = ref.behandlingId();
        if (BehandlingStatus.OPPRETTET.equals(ref.behandlingStatus())) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra VurderKompletthetSteg (en gang) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        var kompletthetManglendeIM = fellesUtil.getInntektsmeldingKomplett(ref, stp);
        if (kompletthetManglendeIM.isPresent()) {
            return kompletthetManglendeIM.get();
        }
        // Denne fristen skulle egentlig vært samordnet med frist over - men man ønsket få opp IM-mangler uavhengig
        if (!kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref).isEmpty()) {
            var ventefristTidligMottattSøknad = fellesUtil.finnVentefristForManglendeVedlegg(behandlingId);
            return ventefristTidligMottattSøknad
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.fristUtløpt());
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        var manglendeVedlegg = kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref);
        return manglendeVedlegg.isEmpty();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref);
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return fellesUtil.hentAlleInntektsmeldingerSomIkkeKommer(ref)
            .stream()
            .map(e -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, e.getArbeidsgiver().getIdentifikator(), true))
            .toList();
    }

    @Override
    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return fellesUtil.vurderEtterlysningInntektsmelding(ref, stp);
    }

}
