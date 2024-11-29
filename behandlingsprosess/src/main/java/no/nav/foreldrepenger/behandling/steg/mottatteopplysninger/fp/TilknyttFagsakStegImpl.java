package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.mottak.sakskompleks.KobleSakerTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.RegistrerFagsakEgenskaper;

@BehandlingStegRef(BehandlingStegType.INNHENT_SØKNADOPP)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class TilknyttFagsakStegImpl implements TilknyttFagsakSteg {

    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private KobleSakerTjeneste kobleSakTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private RegistrerFagsakEgenskaper registrerFagsakEgenskaper;

    TilknyttFagsakStegImpl() {
        // for CDI proxy
    }

    @Inject
    public TilknyttFagsakStegImpl(BehandlingRepositoryProvider provider,
                                  KobleSakerTjeneste kobleSakTjeneste,
                                  BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                  InntektArbeidYtelseTjeneste iayTjeneste,
                                  RegistrerFagsakEgenskaper registrerFagsakEgenskaper) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.ytelsesFordelingRepository = provider.getYtelsesFordelingRepository();
        this.kobleSakTjeneste = kobleSakTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.registrerFagsakEgenskaper = registrerFagsakEgenskaper;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        // Prøve å koble fagsaker
        kobleSakerOppdaterEnhetVedBehov(behandling);

        // Vurder automatisk merking av opptjening utland
        List<AksjonspunktResultat> aksjonspunkter = new ArrayList<>();

        var undersøkeEØS = BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) && harOppgittUtland(kontekst);

        registrerFagsakEgenskaper.fagsakEgenskaperFraSøknad(behandling, undersøkeEØS);

        if (undersøkeEØS && !registrerFagsakEgenskaper.harVurdertInnhentingDokumentasjon(behandling)) {
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AUTOMATISK_MARKERING_AV_UTENLANDSSAK));
        }

        return aksjonspunkter.isEmpty() ? BehandleStegResultat.utførtUtenAksjonspunkter()
                : BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    private boolean harOppgittUtland(BehandlingskontrollKontekst kontekst) {
        return harOppgittUtenlandskInntekt(kontekst.getBehandlingId()) || harOppgittAnnenForelderTilknytningEØS(kontekst.getBehandlingId());
    }

    private boolean harOppgittUtenlandskInntekt(Long behandlingId) {
        var oppgittOpptening = iayTjeneste.finnGrunnlag(behandlingId)
                .flatMap(InntektArbeidYtelseGrunnlag::getGjeldendeOppgittOpptjening);
        return oppgittOpptening.map(oppgittOpptjening -> oppgittOpptjening.getOppgittArbeidsforhold().stream()
                .anyMatch(OppgittArbeidsforhold::erUtenlandskInntekt)).orElse(false);
    }

    private boolean harOppgittAnnenForelderTilknytningEØS(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .filter(YtelseFordelingAggregat::oppgittAnnenForelderTilknytningEØS)
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

        if (kobling.flatMap(FagsakRelasjon::getFagsakNrTo).isPresent()) { // Ble koblet sjekk enhet
            behandlendeEnhetTjeneste.endretBehandlendeEnhetEtterFagsakKobling(behandling)
                .ifPresent(organisasjonsEnhet -> behandlendeEnhetTjeneste
                    .oppdaterBehandlendeEnhet(behandling, organisasjonsEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "Koblet sak"));
        } else if (kobling.isEmpty()) { // Finnes ikke kobling fra før, lag en.
            if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
                kobleSakTjeneste.opprettFagsakRelasjon(behandling.getFagsak());
            } else {
                throw new IllegalStateException("Utviklerfeil: Foreldrepenger revurdering uten relasjon, sak " + behandling.getSaksnummer().getVerdi());
            }
        } else { // Finnes fagsakrelasjon. Skal ikke endre denne dersom det finnes vedtak. Dersom finnes og det ikke er vedtak - lagre ny (uten DG)
            var harVedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId()).isPresent();
            if (!harVedtak && BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) && kobling.get().getDekningsgrad() != null) {
                kobleSakTjeneste.opprettFagsakRelasjon(behandling.getFagsak());
            }
        }
    }
}
