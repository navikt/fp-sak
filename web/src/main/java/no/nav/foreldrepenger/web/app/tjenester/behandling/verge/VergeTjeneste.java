package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.person.verge.OpprettVergeTjeneste;
import no.nav.foreldrepenger.domene.person.verge.VergeDtoTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.OpprettVergeDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.dto.NyVergeDto;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class VergeTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private VergeRepository vergeRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private OpprettVergeTjeneste opprettVergeTjeneste;
    private VergeDtoTjeneste vergeDtoTjeneste;

    @Inject
    public VergeTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                         BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                         VergeRepository vergeRepository,
                         HistorikkinnslagRepository historikkinnslagRepository,
                         BehandlingRepository behandlingRepository,
                         PersonopplysningTjeneste personopplysningTjeneste,
                         OpprettVergeTjeneste opprettVergeTjeneste,
                         VergeDtoTjeneste vergeDtoTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.vergeRepository = vergeRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.opprettVergeTjeneste = opprettVergeTjeneste;
        this.vergeDtoTjeneste = vergeDtoTjeneste;
    }

    VergeTjeneste() {
        //CDI
    }

    public VergeDto hentVerge(Behandling behandling) {
        return vergeRepository.hentAggregat(behandling.getId()).flatMap(vergeDtoTjeneste::lagVergeDto).orElse(null);
    }

    public void opprettVerge(Behandling behandling, NyVergeDto param) {
        opprettVergeTjeneste.opprettVerge(behandling.getId(), behandling.getFagsakId(), map(param));
    }

    public void fjernVerge(Behandling behandling) {
        vergeRepository.fjernVergeFraEksisterendeGrunnlagHvisFinnes(behandling.getId());
        opprettHistorikkinnslagForFjernetVerge(behandling);
        avbrytVergeAksjonspunktHvisFinnes(behandling);
    }

    public VergeBehandlingsmenyEnum utledBehandlingOperasjon(Behandling behandling) {
        var behandlingId = behandling.getId();
        var vergeAggregat = vergeRepository.hentAggregat(behandlingId);
        var harRegistrertVerge = vergeAggregat.isPresent() && vergeAggregat.get().getVerge().isPresent();
        var harÅpentVergeAP = behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE);
        var iForeslåVedtakllerSenereSteg = behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandlingId, BehandlingStegType.FORESLÅ_VEDTAK);
        var iFatteVedtakEllerSenereSteg = behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandlingId, BehandlingStegType.FATTE_VEDTAK);
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


    void opprettVergeAksjonspunktOgHoppTilbakeTilFORVEDSTEGHvisSenereSteg(Behandling behandling) {
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE)) {
            var msg = String.format("Behandling %s har allerede aksjonspunkt 5030 for verge/fullmektig", behandling.getId());
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
        avbrytVergeAksjonspunktHvisFinnes(behandling);
        vergeRepository.fjernVergeFraEksisterendeGrunnlagHvisFinnes(behandling.getId());
        opprettHistorikkinnslagForFjernetVerge(behandling);
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }

    private void avbrytVergeAksjonspunktHvisFinnes(Behandling behandling) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AVKLAR_VERGE)
            .ifPresent(aksjonspunkt -> behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(),
                List.of(aksjonspunkt)));
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

    private OpprettVergeDto map(NyVergeDto dto) {
        return new OpprettVergeDto(dto.getNavn(), dto.getFnr(), dto.getGyldigFom(), dto.getGyldigTom(), dto.getVergeType(),
            dto.getOrganisasjonsnummer(), null);
    }
}
