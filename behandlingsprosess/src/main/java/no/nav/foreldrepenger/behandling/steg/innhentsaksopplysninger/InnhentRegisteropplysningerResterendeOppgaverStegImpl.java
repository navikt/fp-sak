package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles.autopunktAlleredeUtført;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_VERGE;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetModell;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "INREG_AVSL")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentRegisteropplysningerResterendeOppgaverStegImpl implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private FagsakTjeneste fagsakTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private KompletthetModell kompletthetModell;
    private BehandlendeEnhetTjeneste enhetTjeneste;

    InnhentRegisteropplysningerResterendeOppgaverStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentRegisteropplysningerResterendeOppgaverStegImpl(BehandlingRepositoryProvider repositoryProvider,
            FagsakTjeneste fagsakTjeneste,
            PersonopplysningTjeneste personopplysningTjeneste,
            FamilieHendelseTjeneste familieHendelseTjeneste,
            BehandlendeEnhetTjeneste enhetTjeneste,
            KompletthetModell kompletthetModell,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {

        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakTjeneste = fagsakTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kompletthetModell = kompletthetModell;
        this.enhetTjeneste = enhetTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        var etterlysIM = kompletthetModell.vurderKompletthet(ref, List.of(AUTO_VENT_ETTERLYST_INNTEKTSMELDING));
        if (!etterlysIM.erOppfylt()) {
            // Dette autopunktet har tilbakehopp/gjenopptak. Går ut av steget hvis auto
            // utført før frist (manuelt av vent). Utført på/etter frist antas automatisk
            // gjenopptak.
            if (!etterlysIM.erFristUtløpt() && !autopunktAlleredeUtført(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, behandling)) {
                return BehandleStegResultat
                        .utførtMedAksjonspunktResultater(singletonList(opprettForAksjonspunkt(AUTO_VENT_ETTERLYST_INNTEKTSMELDING)));
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

}
