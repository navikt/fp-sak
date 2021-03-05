package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class VergeTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private VergeRepository vergeRepository;
    private HistorikkRepository historikkRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public VergeTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                         BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                         VergeRepository vergeRepository,
                         HistorikkRepository historikkRepository,
                         BehandlingRepository behandlingRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.vergeRepository = vergeRepository;
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
    }

    VergeTjeneste() {
        //CDI
    }

    public VergeBehandlingsmenyDto utledBehandlingsmeny(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<VergeAggregat> vergeAggregat = vergeRepository.hentAggregat(behandlingId);
        boolean harRegistrertVerge = vergeAggregat.isPresent() && vergeAggregat.get().getVerge().isPresent();
        boolean harVergeAksjonspunkt = behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE);
        boolean stårIKofakEllerSenereSteg = behandling.harSattStartpunkt();

        if (!stårIKofakEllerSenereSteg || !behandling.erYtelseBehandling()) {
            return new VergeBehandlingsmenyDto(behandlingId, VergeBehandlingsmenyEnum.SKJUL);
        } else if (!harRegistrertVerge && !harVergeAksjonspunkt) {
            return new VergeBehandlingsmenyDto(behandlingId, VergeBehandlingsmenyEnum.OPPRETT);
        } else {
            return new VergeBehandlingsmenyDto(behandlingId, VergeBehandlingsmenyEnum.FJERN);
        }
    }

    void opprettVergeAksjonspunktOgHoppTilbakeTilKofakHvisSenereSteg(Behandling behandling) {
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE)) {
            var msg = String.format("Behandling %s har allerede aksjonspunkt 5030 for verge/fullmektig",
                behandling.getId());
            throw new TekniskException("FP-185321", msg);
        }
        if (!behandling.erYtelseBehandling()) {
            var msg = String.format("Behandling %s er ikke en ytelsesbehandling -"
                    + " kan ikke registrere verge/fullmektig", behandling.getId());
            throw new TekniskException("FP-185322", msg);
        }
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(AksjonspunktDefinisjon.AVKLAR_VERGE));
        behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling,
            BehandlingStegType.KONTROLLER_FAKTA);
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }

    void fjernVergeGrunnlagOgAksjonspunkt(Behandling behandling) {
        fjernVergeAksjonspunkt(behandling);
        vergeRepository.fjernVergeFraEksisterendeGrunnlag(behandling.getId());
        opprettHistorikkinnslagForFjernetVerge(behandling);
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }

    private void fjernVergeAksjonspunkt(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AVKLAR_VERGE).
            ifPresent(aksjonspunkt -> behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst,
                behandling.getAktivtBehandlingSteg(), List.of(aksjonspunkt)));
    }

    private void opprettHistorikkinnslagForFjernetVerge(Behandling behandling) {
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder().medHendelse(
            HistorikkinnslagType.FJERNET_VERGE);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setType(HistorikkinnslagType.FJERNET_VERGE);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        historikkInnslagTekstBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
