package no.nav.foreldrepenger.mottak.vedtak.avstemming;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappEksterneYtelserTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "vedtak.overlapp.avstem", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VedtakOverlappAvstemSakTask extends GenerellProsessTask {
    public static final String LOG_SAKSNUMMER_KEY = "logsaksnummer";
    public static final String LOG_HENDELSE_KEY = "hendelse";


    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private LoggOverlappEksterneYtelserTjeneste syklogger;

    VedtakOverlappAvstemSakTask() {
        // for CDI proxy
    }

    @Inject
    public VedtakOverlappAvstemSakTask(FagsakRepository fagsakRepository,
                                       BehandlingRepository behandlingRepository,
                                       BeregningsresultatRepository beregningsresultatRepository,
                                       LoggOverlappEksterneYtelserTjeneste syklogger) {
        super();
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.syklogger = syklogger;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var saksnr = prosessTaskData.getPropertyValue(LOG_SAKSNUMMER_KEY);
        var hendelse = Optional.ofNullable(prosessTaskData.getPropertyValue(LOG_HENDELSE_KEY)).orElse(OverlappVedtak.HENDELSE_AVSTEM_SAK);
        if (saksnr != null) {
            loggOverlappOTH(saksnr, hendelse);
        }
    }

    private void loggOverlappOTH(String saksnr, String hendelse) {
        // Finner alle behandlinger med vedtaksdato innen intervall (evt med gitt saksnummer) - tidligste dato = tidligeste dato med utbetaling
        var behandling = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnr))
            .filter(s -> FagsakYtelseType.FORELDREPENGER.equals(s.getYtelseType()) || FagsakYtelseType.SVANGERSKAPSPENGER.equals(s.getYtelseType()))
            .filter(s -> !s.erStengt())
            .flatMap(s -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(s.getId()))
            .filter(this::harUtbetaling);
        behandling.ifPresent(b -> syklogger.loggOverlappForAvstemming(hendelse, b));
    }

    private boolean harUtbetaling(Behandling behandling) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .anyMatch(p -> p.getDagsats() > 0);
    }

}
