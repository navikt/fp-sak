package no.nav.foreldrepenger.mottak.vedtak.avstemming;

import static no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak.HENDELSE_AVSTEM_SPØKELSE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
    public static final String LOG_TEMA_KEY_KEY = "logtema";
    public static final String LOG_TEMA_FOR_KEY = "temaFOR";
    public static final String LOG_TEMA_OTH_KEY = "temaOTH";
    public static final String LOG_TEMA_SPO_KEY = "temaSPO";
    public static final String LOG_TEMA_BOTH_KEY = "temaALL";
    public static final String LOG_FOM_KEY = "logfom";
    public static final String LOG_TOM_KEY = "logtom";
    public static final String LOG_SAKSNUMMER_KEY = "logsaksnummer";


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
        if (saksnr != null) {
            loggOverlappFOR(null, null, saksnr, OverlappVedtak.HENDELSE_AVSTEM_SAK);
            loggOverlappOTH(null, null, saksnr, OverlappVedtak.HENDELSE_AVSTEM_SAK);
        } else {
            var fom = LocalDate.parse(prosessTaskData.getPropertyValue(LOG_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
            var tom = LocalDate.parse(prosessTaskData.getPropertyValue(LOG_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
            if (LOG_TEMA_FOR_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(LOG_TEMA_KEY_KEY))) {
                loggOverlappFOR(fom, tom, saksnr, OverlappVedtak.HENDELSE_AVSTEM_PERIODE);
            } else if (LOG_TEMA_OTH_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(LOG_TEMA_KEY_KEY))) {
                loggOverlappOTH(fom, tom, saksnr, OverlappVedtak.HENDELSE_AVSTEM_PERIODE);
            }  else if (LOG_TEMA_SPO_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(LOG_TEMA_KEY_KEY))) {
                loggOverlappSPO(fom, tom, saksnr);
            }
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

    private void loggOverlappSPO(LocalDate fom, LocalDate tom, String saksnr) {
        if (fom != null) LOG.info("FPSAK DETEKTOR SPokelse PERIODE {} til {}", fom, tom);
        // Finner alle behandlinger med vedtaksdato innen intervall (evt med gitt saksnummer) - tidligste dato = tidligeste dato med utbetaling
        var saker = informasjonssakRepository.finnSakerSisteVedtakInnenIntervallMedKunUtbetalte(fom, tom, saksnr);
        saker.forEach(o -> syklogger.loggOverlappForAvstemmingSpøkelse(HENDELSE_AVSTEM_SPØKELSE, o.getBehandlingId(), o.getSaksnummer(), o.getAktørId()));
    }

}
