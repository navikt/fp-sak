package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("fpinntektsmelding.vurderIMOgOpprettForespørsel")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class vurderIMOgOpprettForespørsel extends GenerellProsessTask {
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste inntektsmeldingMangelTjeneste;

    private static final Logger LOG = LoggerFactory.getLogger(vurderIMOgOpprettForespørsel.class);

    public vurderIMOgOpprettForespørsel() {
        //Cdi
    }

    @Inject
    public vurderIMOgOpprettForespørsel(BehandlingRepository behandlingRepository,
                                        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                        InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                        FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste,
                                        ArbeidsforholdInntektsmeldingMangelTjeneste inntektsmeldingMangelTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
        this.inntektsmeldingMangelTjeneste = inntektsmeldingMangelTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        LOG.info("Starter task for å sjekke om behandling med id: {} mangler IM, og oppretter forespørsel", behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        if(behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD)) {
            inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAreg(ref, stp)
                .keySet()
                .stream()
                .filter(Arbeidsgiver::getErVirksomhet)
                .forEach(a -> fpInntektsmeldingTjeneste.lagForespørselTask(a.getIdentifikator(), ref));
        } else if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING)) {
            inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp)
                .keySet()
                .stream()
                .filter(Arbeidsgiver::getErVirksomhet)
                .forEach(a -> fpInntektsmeldingTjeneste.lagForespørselTask(a.getIdentifikator(), ref));
        } else if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING)) {
            //må sjekke saksbehandler-valg for å finne ut hvilke inntektmeldinger som mangler
            inntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(ref, stp)
                .stream()
                .filter(arbforholdImStatus -> arbforholdImStatus.inntektsmeldingStatus().equals(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT))
                .filter(arbforholdImStatus -> arbforholdImStatus.arbeidsgiver().getErVirksomhet())
                .forEach(ikkeMottattIm -> fpInntektsmeldingTjeneste.lagForespørselTask(ikkeMottattIm.arbeidsgiver().getOrgnr(), ref));
        } else {
            throw new IllegalArgumentException("Behandling har ikke aksjonspunkt 7003, 7030 eller 5085 og er ugyldig for denne operasjonen");
        }
    }
}
