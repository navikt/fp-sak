package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.mottak.sakogenhet.KobleSakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@BehandlingStegRef(kode = "INSØK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class TilknyttFagsakStegImpl implements TilknyttFagsakSteg {

    private BehandlingRepository behandlingRepository;
    private KobleSakTjeneste kobleSakTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    TilknyttFagsakStegImpl() {
        // for CDI proxy
    }

    @Inject
    public TilknyttFagsakStegImpl(BehandlingRepositoryProvider provider, // NOSONAR
                                KobleSakTjeneste kobleSakTjeneste,
                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                OppgaveTjeneste oppgaveTjeneste,
                                InntektArbeidYtelseTjeneste iayTjeneste) {// NOSONAR
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.kobleSakTjeneste = kobleSakTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        avsluttTidligereRegistreringsoppgave(behandling);

        // Prøve å koble fagsaker
        kobleSakerOppdaterEnhetVedBehov(behandling);

        // Vurder automatisk merking av opptjening utland
        List<AksjonspunktResultat> aksjonspunkter = new ArrayList<>();

        if (!behandling.harAksjonspunktMedType(MANUELL_MARKERING_AV_UTLAND_SAKSTYPE) && !behandling.erRevurdering() && harOppgittUtenlandskInntekt(kontekst.getBehandlingId())) {
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AUTOMATISK_MARKERING_AV_UTENLANDSSAK));
        }

        return aksjonspunkter.isEmpty() ? BehandleStegResultat.utførtUtenAksjonspunkter() : BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    private void avsluttTidligereRegistreringsoppgave(Behandling behandling) {
        oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, OppgaveÅrsak.REGISTRER_SØKNAD);
    }

    private boolean harOppgittUtenlandskInntekt(Long behandlingId) {
        Optional<OppgittOpptjening> oppgittOpptening = iayTjeneste.finnGrunnlag(behandlingId)
            .flatMap(InntektArbeidYtelseGrunnlag::getOppgittOpptjening);
        if (!oppgittOpptening.isPresent()) {
            return false;
        }
        return oppgittOpptening.get().getOppgittArbeidsforhold().stream().anyMatch(OppgittArbeidsforhold::erUtenlandskInntekt);
    }

    private void kobleSakerOppdaterEnhetVedBehov(Behandling behandling) {
        FagsakRelasjon kobling = kobleSakTjeneste.finnFagsakRelasjonDersomOpprettet(behandling).orElse(null);
        if (kobling != null && kobling.getFagsakNrTo().isPresent()) {
            // Allerede koblet. Da er vi på gjenvisitt og vi ser heller ikke på annen part fra søknad.
            return;
        }

        kobleSakTjeneste.kobleRelatertFagsakHvisDetFinnesEn(behandling);
        kobling = kobleSakTjeneste.finnFagsakRelasjonDersomOpprettet(behandling).orElse(null);

        if (kobling == null || kobling.getFagsakNrTo().isEmpty()) {
            // Ingen kobling foretatt
            return;
        }
        behandlendeEnhetTjeneste.endretBehandlendeEnhetEtterFagsakKobling(behandling, kobling).ifPresent(organisasjonsEnhet -> behandlendeEnhetTjeneste
            .oppdaterBehandlendeEnhet(behandling, organisasjonsEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "Koblet sak"));
    }
}
