package no.nav.foreldrepenger.mottak.vedtak.avstemming;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappEksterneYtelserTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "vedtak.overlapp.avstem", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VedtakOverlappAvstemSakTask extends GenerellProsessTask {
    public static final String LOG_SAKSNUMMER_KEY = "logsaksnummer";
    public static final String LOG_HENDELSE_KEY = "hendelse";


    private InformasjonssakRepository informasjonssakRepository;
    private LoggOverlappEksterneYtelserTjeneste syklogger;

    VedtakOverlappAvstemSakTask() {
        // for CDI proxy
    }

    @Inject
    public VedtakOverlappAvstemSakTask(InformasjonssakRepository informasjonssakRepository,
                                       LoggOverlappEksterneYtelserTjeneste syklogger) {
        super();
        this.informasjonssakRepository = informasjonssakRepository;
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
        var saker = informasjonssakRepository.finnSakerSisteVedtakInnenIntervallMedKunUtbetalte(null, null, saksnr);
        saker.forEach(o -> syklogger.loggOverlappForAvstemming(hendelse, o.getBehandlingId()));
    }

}
