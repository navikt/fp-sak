package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.TilbakeførTilDekningsgradStegTask;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class AvklarDekningsgradFellesTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private Historikkinnslag2Repository historikkinnslag2Repository;

    @Inject
    public AvklarDekningsgradFellesTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                            FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                            ProsessTaskTjeneste prosessTaskTjeneste,
                                            Historikkinnslag2Repository historikkinnslag2Repository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    AvklarDekningsgradFellesTjeneste() {
        // for CDI proxy
    }

    public OppdateringResultat oppdater(int avklartDekningsgrad, Long fagsakId, Long behandlingId, String begrunnelse) {
        var avklart = Dekningsgrad.grad(avklartDekningsgrad);
        lagreDekningsgrad(avklart, fagsakId, behandlingId);
        opprettHistorikkinnslag(begrunnelse, avklart, fagsakId, behandlingId);
        tilbakeførAnnenpartsSak(fagsakId);


        return OppdateringResultat.utenTransisjon().build();
    }

    private void lagreDekningsgrad(Dekningsgrad avklartDekningsgrad, Long fagsakId, Long behandlingId) {
        ytelseFordelingTjeneste.lagreSakskompleksDekningsgrad(behandlingId, avklartDekningsgrad);
        fagsakRelasjonTjeneste.oppdaterDekningsgrad(fagsakId, avklartDekningsgrad);
    }

    private void opprettHistorikkinnslag(String begrunnelse, Dekningsgrad avklartDekningsgrad, Long fagsakId, Long behandlingId) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medTittel(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .addLinje(new HistorikkinnslagLinjeBuilder().til("Dekningsgrad", avklartDekningsgrad.getVerdi()))
            .addLinje(begrunnelse)
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    private void tilbakeførAnnenpartsSak(Long fagsakId) {
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsakId).orElseThrow();
        fagsakRelasjon.getRelatertFagsakFraId(fagsakId).ifPresent(sak -> {
            var prosessTaskData = ProsessTaskData.forProsessTask(TilbakeførTilDekningsgradStegTask.class);
            prosessTaskData.setFagsak(sak.getSaksnummer().getVerdi(), sak.getId());
            prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(5)); //Liten delay her for å hindre samtidighetsproblem i køing inn i uttak
            prosessTaskTjeneste.lagre(prosessTaskData);
        });
    }
}
