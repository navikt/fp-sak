package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class ForvaltningUttakTjeneste {

    private BehandlingRepository behandlingRepository;
    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FagsakRepository fagsakRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private HistorikkRepository historikkRepository;

    @Inject
    public ForvaltningUttakTjeneste(BehandlingRepository behandlingRepository,
                                    BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                    FagsakRepository fagsakRepository,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                    HistorikkRepository historikkRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
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
        fagsakRelasjonTjeneste.nullstillOverstyrtStønadskontoberegning(fagsak);
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);
    }

    public void setStartdato(UUID behandlingId, LocalDate startdato) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        ytelseFordelingTjeneste.aksjonspunktAvklarStartdatoForPerioden(behandling.getId(), startdato);

        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        historikkinnslag.setBehandlingId(behandling.getId());

        var historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medSkjermlenke(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
            .medEndretFelt(HistorikkEndretFeltType.STARTDATO_FRA_SOKNAD, null, startdato)
            .medBegrunnelse(String.format("FORVALTNING - satt startdato til %s pga manglende uttak", startdato));
        historieBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    public void endreAnnenForelderHarRett(UUID behandlingUUID, boolean harRett) {
        var behandlingId = behandlingRepository.hentBehandling(behandlingUUID).getId();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        if (ytelseFordelingAggregat.getAnnenForelderRettAvklaring() == null) {
            throw new ForvaltningException("Kan ikke endre ettersom annen forelder har rett ikke er avklart");
        }
        if (ytelseFordelingAggregat.harAnnenForelderRett(false) != harRett) {
            ytelseFordelingTjeneste.bekreftAnnenforelderHarRett(behandlingId, harRett, null, ytelseFordelingAggregat.getMorUføretrygdAvklaring());

            var begrunnelse = harRett ? "FORVALTNING - Endret til annen forelder har rett" : "FORVALTNING - Endret til annen forelder har ikke rett";
            lagHistorikkinnslagRett(behandlingId, begrunnelse);
        }
    }

    public void endreAnnenForelderHarRettEØS(UUID behandlingUUID, boolean annenForelderHarRettEØS, boolean annenForelderHarOppholdEØS) {
        var behandling = behandlingRepository.hentBehandling(behandlingUUID);
        var behandlingId = behandling.getId();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        if (!behandling.erRevurdering() && ytelseFordelingAggregat.getAnnenForelderRettEØSAvklaring() != null) {
            throw new ForvaltningException("Kan ikke endre oppgitt EØS rett hvis rett og omsorg allerede er avklart i aksjonspunkt. "
                + "Hopp behandlingen tilbake til tidligere steg for å fjerne avklaringen. Senest steg KONTROLLER_OMSORG_RETT");
        }

        var nyRettighet = new OppgittRettighetEntitet(false, false, false, annenForelderHarRettEØS, annenForelderHarOppholdEØS);
        if (ytelseFordelingAggregat.getOverstyrtRettighet().isEmpty()) {
            ytelseFordelingTjeneste.endreOppgittRettighet(behandlingId, nyRettighet);
        } else {
            ytelseFordelingTjeneste.endreOverstyrtRettighet(behandlingId, nyRettighet);
        }
        var begrunnelse = annenForelderHarRettEØS ? "FORVALTNING - Endret til at bruker har oppgitt at annen forelder har rett i EØS" :
            "FORVALTNING - Endret til at bruker har oppgitt at annen forelder ikke har rett i EØS";
        lagHistorikkinnslagRett(behandlingId, begrunnelse);
    }

    public void endreAleneomsorg(UUID behandlingUuid, boolean aleneomsorg) {
        var behandlingId = behandlingRepository.hentBehandling(behandlingUuid).getId();
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForAleneomsorg(behandlingId, aleneomsorg);

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

    public void endreMorUføretrygd(UUID behandlingUUID, boolean morUføretrygd) {
        var behandling = behandlingRepository.hentBehandling(behandlingUUID);
        var behandlingId = behandling.getId();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        var bareFarRett = !RelasjonsRolleType.erMor(behandling.getRelasjonsRolleType()) &&
            !ytelseFordelingAggregat.harAnnenForelderRett(false);
        if (!bareFarRett) {
            throw new ForvaltningException("Gjelder ikke bare far rett");
        }
        if (!behandling.erRevurdering() && ytelseFordelingAggregat.getMorUføretrygdAvklaring() != null) {
            throw new ForvaltningException("Kan ikke endre oppgitt Uføretrygd rett hvis rett og omsorg allerede er avklart i aksjonspunkt. "
                + "Hopp behandlingen tilbake til tidligere steg for å fjerne avklaringen. Senest steg KONTROLLER_OMSORG_RETT");
        }

        var nyRettighet = new OppgittRettighetEntitet(false, false, morUføretrygd, false, null);
        if (ytelseFordelingAggregat.getOverstyrtRettighet().isEmpty()) {
            ytelseFordelingTjeneste.endreOppgittRettighet(behandlingId, nyRettighet);
        } else {
            ytelseFordelingTjeneste.endreOverstyrtRettighet(behandlingId, nyRettighet);
        }
        var begrunnelse = morUføretrygd ? "FORVALTNING - Endret til at mor mottar Uføretrygd" :
            "FORVALTNING - Endret til at mor ikke mottar Uføretrygd";
        lagHistorikkinnslagRett(behandlingId, begrunnelse);
    }

    private void lagHistorikkinnslagRett(Long behandlingId, String begrunnelse) {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        historikkinnslag.setBehandlingId(behandlingId);

        var historieBuilder = new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medBegrunnelse(begrunnelse);
        historieBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
