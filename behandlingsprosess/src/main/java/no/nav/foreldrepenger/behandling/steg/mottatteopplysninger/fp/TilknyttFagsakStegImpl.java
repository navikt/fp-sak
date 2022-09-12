package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE;

import java.util.ArrayList;
import java.util.List;

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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.mottak.sakskompleks.KobleSakerTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@BehandlingStegRef(BehandlingStegType.INNHENT_SØKNADOPP)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class TilknyttFagsakStegImpl implements TilknyttFagsakSteg {

    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private KobleSakerTjeneste kobleSakTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    TilknyttFagsakStegImpl() {
        // for CDI proxy
    }

    @Inject
    public TilknyttFagsakStegImpl(BehandlingRepositoryProvider provider, // NOSONAR
            KobleSakerTjeneste kobleSakTjeneste,
            BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
            OppgaveTjeneste oppgaveTjeneste,
            InntektArbeidYtelseTjeneste iayTjeneste) {// NOSONAR
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.ytelsesFordelingRepository = provider.getYtelsesFordelingRepository();
        this.kobleSakTjeneste = kobleSakTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        avsluttTidligereRegistreringsoppgave(behandling);

        // Prøve å koble fagsaker
        kobleSakerOppdaterEnhetVedBehov(behandling);

        // Vurder automatisk merking av opptjening utland
        List<AksjonspunktResultat> aksjonspunkter = new ArrayList<>();

        if (!behandling.harAksjonspunktMedType(MANUELL_MARKERING_AV_UTLAND_SAKSTYPE) && !behandling.erRevurdering()
                && (harOppgittUtenlandskInntekt(kontekst.getBehandlingId()) || harOppgittAnnenForelderHarOpptjentRettFraEØS(kontekst.getBehandlingId()))) {
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AUTOMATISK_MARKERING_AV_UTENLANDSSAK));
        }

        return aksjonspunkter.isEmpty() ? BehandleStegResultat.utførtUtenAksjonspunkter()
                : BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    private void avsluttTidligereRegistreringsoppgave(Behandling behandling) {
        oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, OppgaveÅrsak.REGISTRER_SØKNAD);
    }

    private boolean harOppgittUtenlandskInntekt(Long behandlingId) {
        var oppgittOpptening = iayTjeneste.finnGrunnlag(behandlingId)
                .flatMap(InntektArbeidYtelseGrunnlag::getOppgittOpptjening);
        return oppgittOpptening.map(oppgittOpptjening -> oppgittOpptjening.getOppgittArbeidsforhold().stream()
                .anyMatch(OppgittArbeidsforhold::erUtenlandskInntekt)).orElse(false);
    }

    private boolean harOppgittAnnenForelderHarOpptjentRettFraEØS(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getOppgittRettighet)
            .filter(OppgittRettighetEntitet::getAnnenForelderRettEØS)
            .isPresent();
    }

    private void kobleSakerOppdaterEnhetVedBehov(Behandling behandling) {
        if (kobleSakTjeneste.finnFagsakRelasjonDersomOpprettet(behandling).flatMap(FagsakRelasjon::getFagsakNrTo).isPresent()) {
            // Allerede koblet. Da er vi på gjenvisitt og vi ser heller ikke på annen part
            // fra søknad.
            return;
        }

        kobleSakTjeneste.kobleRelatertFagsakHvisDetFinnesEn(behandling);
        var kobling = kobleSakTjeneste.finnFagsakRelasjonDersomOpprettet(behandling);

        if (kobling.isEmpty() || kobling.flatMap(FagsakRelasjon::getFagsakNrTo).isEmpty()) {
            // Opprett eller oppdater relasjon hvis førstegangsbehandling. Ellers arves
            // dekningsgrad fra koblet sak.
            if (!behandling.erRevurdering()) {
                int dekningsgradVerdi = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
                        .map(YtelseFordelingAggregat::getOppgittDekningsgrad)
                        .map(OppgittDekningsgradEntitet::getDekningsgrad).orElse(Dekningsgrad._100.getVerdi());
                kobleSakTjeneste.oppdaterFagsakRelasjonMedDekningsgrad(behandling.getFagsak(), kobling, Dekningsgrad.grad(dekningsgradVerdi));
            }
            // Ingen kobling foretatt
            return;
        }
        behandlendeEnhetTjeneste.endretBehandlendeEnhetEtterFagsakKobling(behandling, kobling.get())
                .ifPresent(organisasjonsEnhet -> behandlendeEnhetTjeneste
                        .oppdaterBehandlendeEnhet(behandling, organisasjonsEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "Koblet sak"));
    }
}
