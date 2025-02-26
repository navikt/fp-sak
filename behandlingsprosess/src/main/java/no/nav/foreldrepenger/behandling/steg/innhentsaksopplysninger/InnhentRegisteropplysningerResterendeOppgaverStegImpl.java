package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_VERGE;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.impl.EtterlysInntektsmeldingTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.INREG_AVSL)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentRegisteropplysningerResterendeOppgaverStegImpl implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private FagsakTjeneste fagsakTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Kompletthetsjekker kompletthetsjekker;
    private BehandlendeEnhetTjeneste enhetTjeneste;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private EtterlysInntektsmeldingTjeneste etterlysInntektsmeldingTjeneste;

    InnhentRegisteropplysningerResterendeOppgaverStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentRegisteropplysningerResterendeOppgaverStegImpl(BehandlingRepository behandlingRepository,
                                                                 FagsakTjeneste fagsakTjeneste,
                                                                 PersonopplysningTjeneste personopplysningTjeneste,
                                                                 FamilieHendelseTjeneste familieHendelseTjeneste,
                                                                 BehandlendeEnhetTjeneste enhetTjeneste,
                                                                 Kompletthetsjekker kompletthetsjekker,
                                                                 FagsakEgenskapRepository fagsakEgenskapRepository,
                                                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                                 EtterlysInntektsmeldingTjeneste etterlysInntektsmeldingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakTjeneste = fagsakTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kompletthetsjekker = kompletthetsjekker;
        this.enhetTjeneste = enhetTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.etterlysInntektsmeldingTjeneste = etterlysInntektsmeldingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        oppdaterFagsakEgenskaper(behandling);

        var etterlysIM = kompletthetsjekker.vurderEtterlysningInntektsmelding(ref, skjæringstidspunkter);
        if (!etterlysIM.erOppfylt() && !skalPassereUtenBrevEtterlysInntektsmelding(behandling)) {
            // Dette autopunktet har tilbakehopp/gjenopptak. Går ut av steget hvis auto utført før frist (manuelt av vent).
            // Utført på/etter frist antas automatisk gjenopptak.
            etterlysInntektsmeldingTjeneste.etterlysInntektsmeldingHvisIkkeAlleredeSendt(ref); // Etterlys inntektsmelding alltid ved mangler!
            if (!etterlysIM.erFristUtløpt() && !VurderKompletthetStegFelles.autopunktAlleredeUtført(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, behandling)) {
                var ar = opprettForAksjonspunktMedFrist(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, Venteårsak.VENT_OPDT_INNTEKTSMELDING, etterlysIM.ventefrist());
                return BehandleStegResultat.utførtMedAksjonspunktResultat(ar);
            }
        }

        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        var barnSøktStønadFor = familieHendelseTjeneste.finnBarnSøktStønadFor(ref, personopplysninger);

        fagsakTjeneste.oppdaterFagsak(behandling, personopplysninger, barnSøktStønadFor);

        enhetTjeneste.sjekkEnhetEtterEndring(behandling)
                .ifPresent(e -> enhetTjeneste.oppdaterBehandlendeEnhet(behandling, e, HistorikkAktør.VEDTAKSLØSNINGEN, "Personopplysning"));

        return erSøkerUnder18ar(ref) ? BehandleStegResultat.utførtMedAksjonspunkter(List.of(AVKLAR_VERGE)) : BehandleStegResultat.utførtUtenAksjonspunkter();

    }

    private boolean erSøkerUnder18ar(BehandlingReferanse ref) {
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        var søker = personopplysninger.getSøker();
        return søker.getFødselsdato().isAfter(LocalDate.now().minusYears(18));
    }

    private boolean skalPassereUtenBrevEtterlysInntektsmelding(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(BehandlingÅrsakType.årsakerRelatertTilDød()::contains);
    }

    private void oppdaterFagsakEgenskaper(Behandling behandling) {
        var dødsrelatert = behandling.getBehandlingÅrsaker().stream().map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(bat -> BehandlingÅrsakType.årsakerRelatertTilDød().contains(bat));
        if (dødsrelatert) {
            fagsakEgenskapRepository.leggTilFagsakMarkering(behandling.getFagsakId(), FagsakMarkering.DØD_DØDFØDSEL);
        }
    }

}
