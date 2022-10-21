package no.nav.foreldrepenger.mottak.vedtak.avstemming;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class VedtakOverlappAvstemTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(VedtakOverlappAvstemTask.class);
    public static final String LOG_SAKSNUMMER_KEY = "logsaksnummer";
    public static final String LOG_HENDELSE_KEY = "hendelse";


    private InformasjonssakRepository informasjonssakRepository;
    private LoggOverlappEksterneYtelserTjeneste syklogger;
    private LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste;

    VedtakOverlappAvstemTask() {
        // for CDI proxy
    }

    @Inject
    public VedtakOverlappAvstemTask(InformasjonssakRepository informasjonssakRepository,
                                    LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste,
                                    LoggOverlappEksterneYtelserTjeneste syklogger) {
        super();
        this.informasjonssakRepository = informasjonssakRepository;
        this.syklogger = syklogger;
        this.loggertjeneste = loggertjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var saksnr = prosessTaskData.getPropertyValue(LOG_SAKSNUMMER_KEY);
        var hendelse = Optional.ofNullable(prosessTaskData.getPropertyValue(LOG_HENDELSE_KEY)).orElse(OverlappVedtak.HENDELSE_AVSTEM_SAK);
        if (saksnr != null) {
            loggOverlappFOR(null, null, saksnr, hendelse);
            loggOverlappOTH(null, null, saksnr, hendelse);
        }
    }

    private void loggOverlappFOR(LocalDate fom, LocalDate tom, String saksnr, String hendelse) {
        if (fom != null) LOG.info("FPSAK DETEKTOR FPSV PERIODE {} til {}", fom, tom);
        // Finner alle behandlinger med vedtaksdato innen intervall (evt med gitt saksnummer) - tidligste dato = Første uttaksdato
        var saker = informasjonssakRepository.finnSakerSisteVedtakInnenIntervallMedSisteVedtak(fom, tom, saksnr);
        saker.forEach(o -> loggertjeneste.vurderOglagreEventueltOverlapp(hendelse, o));
    }

    private void loggOverlappOTH(LocalDate fom, LocalDate tom, String saksnr, String hendelse) {
        if (fom != null) LOG.info("FPSAK DETEKTOR SPBS PERIODE {} til {}", fom, tom);
        // Finner alle behandlinger med vedtaksdato innen intervall (evt med gitt saksnummer) - tidligste dato = tidligeste dato med utbetaling
        var saker = informasjonssakRepository.finnSakerSisteVedtakInnenIntervallMedKunUtbetalte(fom, tom, saksnr);
        saker.forEach(o -> syklogger.loggOverlappForAvstemming(hendelse, o.getBehandlingId(), o.getSaksnummer(), o.getAktørId()));
    }

}
