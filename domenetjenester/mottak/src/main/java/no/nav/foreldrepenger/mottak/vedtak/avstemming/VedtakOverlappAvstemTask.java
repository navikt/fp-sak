package no.nav.foreldrepenger.mottak.vedtak.avstemming;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(VedtakOverlappAvstemTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VedtakOverlappAvstemTask extends GenerellProsessTask {
    public static final String TASKTYPE = "vedtak.overlapp.avstem";

    private static final Logger LOG = LoggerFactory.getLogger(VedtakOverlappAvstemTask.class);
    public static final String LOG_TEMA_KEY_KEY = "logtema";
    public static final String LOG_TEMA_FOR_KEY = "temaFOR";
    public static final String LOG_TEMA_OTH_KEY = "temaOTH";
    public static final String LOG_TEMA_BOTH_KEY = "temaALL";
    public static final String LOG_PREFIX_KEY = "logprefix";
    public static final String LOG_FOM_KEY = "logfom";
    public static final String LOG_TOM_KEY = "logtom";
    public static final String LOG_SAKSNUMMER_KEY = "logsaksnummer";


    private InformasjonssakRepository informasjonssakRepository;
    private IdentifiserOverlappendeInfotrygdYtelseTjeneste syklogger;
    private LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste;

    VedtakOverlappAvstemTask() {
        // for CDI proxy
    }

    @Inject
    public VedtakOverlappAvstemTask(InformasjonssakRepository informasjonssakRepository,
                                    LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste,
                                    IdentifiserOverlappendeInfotrygdYtelseTjeneste syklogger) {
        super();
        this.informasjonssakRepository = informasjonssakRepository;
        this.syklogger = syklogger;
        this.loggertjeneste = loggertjeneste;

    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        String saksnr = prosessTaskData.getPropertyValue(LOG_SAKSNUMMER_KEY);
        String prefix = prosessTaskData.getPropertyValue(LOG_PREFIX_KEY);
        if (saksnr != null) {
            loggOverlappFOR(null, null, saksnr, prefix);
            loggOverlappOTH(null, null, saksnr, prefix);
        } else {
            LocalDate fom = LocalDate.parse(prosessTaskData.getPropertyValue(LOG_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate tom = LocalDate.parse(prosessTaskData.getPropertyValue(LOG_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
            if (LOG_TEMA_FOR_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(LOG_TEMA_KEY_KEY))) {
                loggOverlappFOR(fom, tom, saksnr, prefix);
            } else if (LOG_TEMA_OTH_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(LOG_TEMA_KEY_KEY))) {
                loggOverlappOTH(fom, tom, saksnr, prefix);
            }
        }
    }

    private void loggOverlappFOR(LocalDate fom, LocalDate tom, String saksnr, String prefix) {
        if (fom != null) LOG.info("FPSAK DETEKTOR FPSV PERIODE {} til {}", fom, tom);
        var saker = informasjonssakRepository.finnSakerOpprettetInnenIntervallMedSisteVedtak(fom, tom, saksnr);
        saker.forEach(o -> loggertjeneste.vurderOglagreEventueltOverlapp(prefix, o.getBehandlingId(), o.getAnnenPartAktørId(), o.getTidligsteDato()));
    }

    private void loggOverlappOTH(LocalDate fom, LocalDate tom, String saksnr, String prefix) {
        if (fom != null) LOG.info("FPSAK DETEKTOR SPBS PERIODE {} til {}", fom, tom);
        var saker = informasjonssakRepository.finnSakerOpprettetInnenIntervallMedKunUtbetalte(fom, tom, saksnr);
        saker.forEach(o -> syklogger.vurderOglagreEventueltOverlapp(prefix, o.getBehandlingId(), o.getTidligsteDato()));
    }

}
