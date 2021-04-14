package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderDekningsgradDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderDekningsgradOppdaterer implements AksjonspunktOppdaterer<VurderDekningsgradDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    VurderDekningsgradOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderDekningsgradOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                        BehandlingRepositoryProvider repositoryProvider,
                                        FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
    }

    @Override
    public OppdateringResultat oppdater(VurderDekningsgradDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();
        var dekningsgradFraDto = dto.getDekningsgrad();
        var gjeldendeDekningsgrad = fagsakRelasjonTjeneste.finnRelasjonFor(behandling.getFagsak()).getGjeldendeDekningsgrad();
        var erDekningsgradEndret = gjeldendeDekningsgrad.getVerdi() != dekningsgradFraDto;
        if (erDekningsgradEndret) {
            oppdaterDekningsgrad(behandling, dekningsgradFraDto);
        }
        lagHistorikkinnslagHvisEndring(param, gjeldendeDekningsgrad.getVerdi(), dto.getDekningsgrad(), erDekningsgradEndret, dto.getBegrunnelse());
        return OppdateringResultat.utenOveropp();
    }

    private void oppdaterDekningsgrad(Behandling behandling, int dekningsgradFraDto) {
        oppdaterBehandlingsresultat(behandling);
        oppdaterFagsakRelasjon(behandling, dekningsgradFraDto);
    }

    private void oppdaterFagsakRelasjon(Behandling behandling, int dekningsgradFraDto) {
        var nyDekningsgrad = Dekningsgrad.grad(dekningsgradFraDto);
        fagsakRelasjonTjeneste.overstyrDekningsgrad(behandling.getFagsak(), nyDekningsgrad);
    }

    private void oppdaterBehandlingsresultat(Behandling behandling) {
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatRepository.hent(behandling.getId()))
            .medEndretDekningsgrad(true)
            .buildFor(behandling);

        behandlingRepository.lagre(behandling, behandlingLåsRepository.taLås(behandling.getId()));
    }

    private void lagHistorikkinnslagHvisEndring(AksjonspunktOppdaterParameter param, int gammelDekningsgrad, int nyDekningsgrad, boolean erDekningsgradEndret, String begrunnelse) {
        historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        var historikkDeler = historikkAdapter.tekstBuilder().getHistorikkinnslagDeler();
        var erBegrunnelseEndret = param.erBegrunnelseEndret();
        if (erBegrunnelseEndret || erDekningsgradEndret) {
            var fraVerdi = gammelDekningsgrad + "%";
            var tilVerdi = nyDekningsgrad + "%";
            historikkAdapter.tekstBuilder().medBegrunnelse(begrunnelse);
            if (!erDekningsgradEndret) {
                historikkAdapter.tekstBuilder().medEndretFeltBegrunnelse(HistorikkEndretFeltType.DEKNINGSGRAD, fraVerdi, tilVerdi);
            } else {
                historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD, fraVerdi, tilVerdi);
            }
            var erSkjermlenkeSatt = historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
            if (!erSkjermlenkeSatt) {
                historikkAdapter.tekstBuilder().medSkjermlenke(SkjermlenkeType.BEREGNING_FORELDREPENGER);
            }
        }
        historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
    }

}
