package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class AvklarDekningsgradFellesTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public AvklarDekningsgradFellesTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                            FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                            HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                            BehandlingsresultatRepository behandlingsresultatRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    AvklarDekningsgradFellesTjeneste() {
        // for CDI proxy
    }

    public OppdateringResultat oppdater(int avklartDekningsgrad, Long fagsakId, Long behandlingId, String begrunnelse) {
        var avklart = Dekningsgrad.grad(avklartDekningsgrad);
        lagreDekningsgrad(avklart, fagsakId, behandlingId);
        opprettHistorikkinnslag(begrunnelse, avklart);
        return OppdateringResultat.utenTransisjon().build();
    }

    private void lagreDekningsgrad(Dekningsgrad avklartDekningsgrad, Long fagsakId, Long behandlingId) {
        ytelseFordelingTjeneste.lagreSakskompleksDekningsgrad(behandlingId, avklartDekningsgrad);
        fagsakRelasjonTjeneste.oppdaterDekningsgrad(fagsakId, avklartDekningsgrad);
        behandlingsresultatRepository.lagre(behandlingId,
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultatRepository.hent(behandlingId)).medEndretDekningsgrad(true).build());
    }

    private void opprettHistorikkinnslag(String begrunnelse, Dekningsgrad avklartDekningsgrad) {
        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse)
            .medSkjermlenke(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
            .medEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD, null, avklartDekningsgrad.getVerdi());
    }
}
