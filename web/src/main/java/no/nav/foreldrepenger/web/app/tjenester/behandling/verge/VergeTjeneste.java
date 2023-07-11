package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class VergeTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private VergeRepository vergeRepository;
    private HistorikkRepository historikkRepository;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    public VergeTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                         BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                         VergeRepository vergeRepository,
                         HistorikkRepository historikkRepository,
                         BehandlingRepository behandlingRepository,
                         SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                         PersonopplysningTjeneste personopplysningTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.vergeRepository = vergeRepository;
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    VergeTjeneste() {
        //CDI
    }

    public VergeBehandlingsmenyDto utledBehandlingsmeny(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var vergeAggregat = vergeRepository.hentAggregat(behandlingId);
        var harRegistrertVerge = vergeAggregat.isPresent() && vergeAggregat.get().getVerge().isPresent();
        var harVergeAksjonspunkt = behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE);
        var iForeslåVedtakllerSenereSteg = behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandlingId, BehandlingStegType.FORESLÅ_VEDTAK);
        var iFatteVedtakEllerSenereSteg = behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandlingId, BehandlingStegType.FATTE_VEDTAK);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var behandlingReferanse = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        var under18År = erSøkerUnder18ar(behandlingReferanse);

        if (harRegistrertVerge && under18År && iForeslåVedtakllerSenereSteg || SpesialBehandling.erSpesialBehandling(behandling)
            || iFatteVedtakEllerSenereSteg) {
            return new VergeBehandlingsmenyDto(behandlingId, VergeBehandlingsmenyEnum.SKJUL);
        }
        if (!harRegistrertVerge && !harVergeAksjonspunkt) {
            return new VergeBehandlingsmenyDto(behandlingId, VergeBehandlingsmenyEnum.OPPRETT);
        }
        return new VergeBehandlingsmenyDto(behandlingId, VergeBehandlingsmenyEnum.FJERN);
    }

    void opprettVergeAksjonspunktOgHoppTilbakeTilFORVEDSTEGHvisSenereSteg(Behandling behandling) {
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE)) {
            var msg = String.format("Behandling %s har allerede aksjonspunkt 5030 for verge/fullmektig",
                behandling.getId());
            throw new TekniskException("FP-185321", msg);
        }

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(AksjonspunktDefinisjon.AVKLAR_VERGE));

        if (behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.FORESLÅ_VEDTAK)) {
            behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, BehandlingStegType.FORESLÅ_VEDTAK);
        }

        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }

    void fjernVergeGrunnlagOgAksjonspunkt(Behandling behandling) {
        fjernVergeAksjonspunkt(behandling);
        vergeRepository.fjernVergeFraEksisterendeGrunnlag(behandling.getId());
        opprettHistorikkinnslagForFjernetVerge(behandling);
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }

    private void fjernVergeAksjonspunkt(Behandling behandling) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AVKLAR_VERGE).
            ifPresent(aksjonspunkt -> behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst,
                behandling.getAktivtBehandlingSteg(), List.of(aksjonspunkt)));
    }

    private void opprettHistorikkinnslagForFjernetVerge(Behandling behandling) {
        var historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder().medHendelse(
            HistorikkinnslagType.FJERNET_VERGE);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setType(HistorikkinnslagType.FJERNET_VERGE);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        historikkInnslagTekstBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    private boolean erSøkerUnder18ar(BehandlingReferanse ref) {
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref)
            .map(PersonopplysningerAggregat::getSøker)
            .map(PersonopplysningEntitet::getFødselsdato)
            .filter(d -> d.isAfter(LocalDate.now().minusYears(18))).isPresent();
    }
}
