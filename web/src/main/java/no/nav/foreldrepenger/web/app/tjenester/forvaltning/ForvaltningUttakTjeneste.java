package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.BekreftFaktaForOmsorgVurderingAksjonspunktDto;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class ForvaltningUttakTjeneste {

    private BehandlingRepository behandlingRepository;
    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRepository fagsakRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private HistorikkRepository historikkRepository;

    @Inject
    public ForvaltningUttakTjeneste(BehandlingRepository behandlingRepository,
                                    BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    FagsakRelasjonRepository fagsakRelasjonRepository,
                                    FagsakRepository fagsakRepository,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                    HistorikkRepository historikkRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.fagsakRepository = fagsakRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.historikkRepository = historikkRepository;
    }

    ForvaltningUttakTjeneste() {
        //CDI
    }

    public void beregnKontoer(UUID behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var input = uttakInputTjeneste.lagInput(behandling);
        var fagsak = fagsakRepository.finnEksaktFagsak(input.getBehandlingReferanse().fagsakId());
        fagsakRelasjonRepository.nullstillOverstyrtStønadskontoberegning(fagsak);
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);
    }

    public void endreAnnenForelderHarRett(UUID behandlingUUID, boolean harRett) {
        var behandlingId = behandlingRepository.hentBehandling(behandlingUUID).getId();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        if (ytelseFordelingAggregat.getPerioderAnnenforelderHarRett().isEmpty()) {
            throw new ForvaltningException("Kan ikke endre ettersom annen forelder har rett ikke er avklart");
        }
        if (UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat, Optional.empty()) != harRett) {
            ytelseFordelingTjeneste.bekreftAnnenforelderHarRett(behandlingId, harRett);

            lagHistorikkinnslagRett(harRett, behandlingId);
        }
    }

    public void endreAleneomsorg(UUID behandlingUuid, Boolean aleneomsorg) {
        var behandlingId = behandlingRepository.hentBehandling(behandlingUuid).getId();
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandlingId, new BekreftFaktaForOmsorgVurderingAksjonspunktDto(aleneomsorg,
            null, null));

        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        historikkinnslag.setBehandlingId(behandlingId);

        var begrunnelse = aleneomsorg ? "FORVALTNING - Endret til aleneomsorg" : "FORVALTNING - Endret til ikke aleneomsorg";
        var historieBuilder = new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medBegrunnelse(begrunnelse);
        historieBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    private void lagHistorikkinnslagRett(boolean harRett, Long behandlingId) {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        historikkinnslag.setBehandlingId(behandlingId);

        var begrunnelse = harRett ? "FORVALTNING - Endret til annen forelder har rett" : "FORVALTNING - Endret til annen forelder har ikke rett";
        var historieBuilder = new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medBegrunnelse(begrunnelse);
        historieBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
