package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.events.SakensPersonerEndretEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.person.verge.OpprettVergeTjeneste;
import no.nav.foreldrepenger.domene.person.verge.VergeDtoTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class VergeTjeneste {

    private AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private VergeRepository vergeRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private OpprettVergeTjeneste opprettVergeTjeneste;
    private VergeDtoTjeneste vergeDtoTjeneste;
    private BehandlingEventPubliserer behandlingEventPubliserer;

    @Inject
    public VergeTjeneste(AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste,
                         BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                         VergeRepository vergeRepository,
                         HistorikkinnslagRepository historikkinnslagRepository,
                         PersonopplysningTjeneste personopplysningTjeneste,
                         OpprettVergeTjeneste opprettVergeTjeneste,
                         VergeDtoTjeneste vergeDtoTjeneste,
                         BehandlingEventPubliserer behandlingEventPubliserer) {
        this.aksjonspunktkontrollTjeneste = aksjonspunktkontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.vergeRepository = vergeRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.opprettVergeTjeneste = opprettVergeTjeneste;
        this.vergeDtoTjeneste = vergeDtoTjeneste;
        this.behandlingEventPubliserer = behandlingEventPubliserer;
    }

    VergeTjeneste() {
        //CDI
    }

    public VergeDto hentVerge(Behandling behandling) {
        return vergeRepository.hentAggregat(behandling.getId()).flatMap(vergeDtoTjeneste::lagVergeDto).orElse(null);
    }

    public void opprettVerge(Behandling behandling, VergeDto param, String begrunnelse) {
        opprettVergeTjeneste.opprettVerge(behandling.getId(), behandling.getFagsakId(), param, begrunnelse);
        if (param.fnr() != null) {
            behandlingEventPubliserer.publiserBehandlingEvent(new SakensPersonerEndretEvent(behandling, "Verge"));
        }
    }

    public void fjernVerge(Behandling behandling, BehandlingLås skriveLås) {
        var harPersonVerge = vergeRepository.hentAggregat(behandling.getId()).flatMap(VergeAggregat::getAktørId).isPresent();
        vergeRepository.fjernVergeFraEksisterendeGrunnlagHvisFinnes(behandling.getId());
        opprettHistorikkinnslagForFjernetVerge(behandling);
        avbrytVergeAksjonspunktHvisFinnes(behandling, skriveLås);
        if (harPersonVerge) {
            behandlingEventPubliserer.publiserBehandlingEvent(new SakensPersonerEndretEvent(behandling, "Verge"));
        }
    }

    public VergeBehandlingsmenyEnum utledBehandlingOperasjon(Behandling behandling) {
        var behandlingId = behandling.getId();
        var vergeAggregat = vergeRepository.hentAggregat(behandlingId);
        var harRegistrertVerge = vergeAggregat.isPresent() && vergeAggregat.get().getVerge().isPresent();
        var harÅpentVergeAP = behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE);
        var iForeslåVedtakllerSenereSteg = !behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FORESLÅ_VEDTAK);
        var iFatteVedtakEllerSenereSteg = !behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FATTE_VEDTAK);
        var under18År = erSøkerUnder18ar(behandlingId, behandling.getAktørId());

        if (harRegistrertVerge && under18År && iForeslåVedtakllerSenereSteg || SpesialBehandling.erSpesialBehandling(behandling)
            || iFatteVedtakEllerSenereSteg || harÅpentVergeAP) {
            return VergeBehandlingsmenyEnum.SKJUL;
        }
        if (!harRegistrertVerge) {
            return VergeBehandlingsmenyEnum.OPPRETT;
        }
        return VergeBehandlingsmenyEnum.FJERN;
    }

    private void avbrytVergeAksjonspunktHvisFinnes(Behandling behandling, BehandlingLås skriveLås) {
        behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AVKLAR_VERGE)
            .map(List::of)
            .ifPresent(loa -> aksjonspunktkontrollTjeneste.lagreAksjonspunkterAvbrutt(behandling, skriveLås, loa));
    }

    private void opprettHistorikkinnslagForFjernetVerge(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel("Opplysninger om verge/fullmektig er fjernet")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private boolean erSøkerUnder18ar(Long behandlingId, AktørId aktørId) {
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(behandlingId, aktørId)
            .map(PersonopplysningerAggregat::getSøker)
            .map(PersonopplysningEntitet::getFødselsdato)
            .filter(d -> d.isAfter(LocalDate.now().minusYears(18)))
            .isPresent();
    }
}
