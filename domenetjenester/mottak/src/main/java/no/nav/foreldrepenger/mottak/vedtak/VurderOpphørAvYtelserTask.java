package no.nav.foreldrepenger.mottak.vedtak;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp.LoggHistoriskOverlappFPInfotrygdVLTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(VurderOpphørAvYtelserTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOpphørAvYtelserTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.vurderOpphørAvYtelser";

    private static final Logger LOG = LoggerFactory.getLogger(VurderOpphørAvYtelserTask.class);
    public static final String HIJACK_KEY_KEY = "hijack";
    public static final String HIJACK_FOR_KEY = "hijackFOR";
    public static final String HIJACK_OTH_KEY = "hijackOTH";
    public static final String HIJACK_PREFIX_KEY = "hijackprefix";
    public static final String HIJACK_FOM_KEY = "hijackfom";
    public static final String HIJACK_TOM_KEY = "hijacktom";
    public static final String HIJACK_SAKSNUMMER_KEY = "hijacksaksnummer";
    public static final String K9_YTELSE_KEY = "k9ytelse";
    public static final String K9_SAK_KEY = "k9sak";
    public static final String K9_FOM_KEY = "k9fom";
    public static final String K9_TOM_KEY = "k9tom";

    private InformasjonssakRepository informasjonssakRepository;
    private IdentifiserOverlappendeInfotrygdYtelseTjeneste syklogger;
    private LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste;



    private VurderOpphørAvYtelser tjeneste;

    VurderOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOpphørAvYtelserTask(VurderOpphørAvYtelser tjeneste,
                                     InformasjonssakRepository informasjonssakRepository,
                                     LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste,
                                     IdentifiserOverlappendeInfotrygdYtelseTjeneste syklogger) {
        this.tjeneste = tjeneste;
        this.informasjonssakRepository = informasjonssakRepository;
        this.syklogger = syklogger;
        this.loggertjeneste = loggertjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (prosessTaskData.getPropertyValue(HIJACK_KEY_KEY) != null) {
            String saksnr = prosessTaskData.getPropertyValue(HIJACK_SAKSNUMMER_KEY);
            String prefix = prosessTaskData.getPropertyValue(HIJACK_PREFIX_KEY);
            LocalDate fom = prosessTaskData.getPropertyValue(HIJACK_FOM_KEY) != null ? LocalDate.parse(prosessTaskData.getPropertyValue(HIJACK_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE) : null;
            LocalDate tom = prosessTaskData.getPropertyValue(HIJACK_TOM_KEY) != null ? LocalDate.parse(prosessTaskData.getPropertyValue(HIJACK_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE) : null;
            if (HIJACK_FOR_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(HIJACK_KEY_KEY))) {
                loggOverlappFOR(fom, tom, saksnr, prefix);
            } else if (HIJACK_OTH_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(HIJACK_KEY_KEY))) {
                loggOverlappOTH(fom, tom, saksnr, prefix);
            }
            return;
        }
        if (prosessTaskData.getPropertyValue(K9_SAK_KEY) != null) {
            String saksnr = prosessTaskData.getPropertyValue(K9_SAK_KEY);
            String ytelse = prosessTaskData.getPropertyValue(K9_YTELSE_KEY);
            LocalDate fom = LocalDate.parse(prosessTaskData.getPropertyValue(K9_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate tom = LocalDate.parse(prosessTaskData.getPropertyValue(K9_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
            LOG.info("Vedtatt-Ytelse task skal opprette VKY for {} {} {} {}", ytelse, saksnr, fom, tom);
            return;
        }
        Long behandlingId = prosessTaskData.getBehandlingId();
        tjeneste.vurderOpphørAvYtelser(prosessTaskData.getFagsakId(), behandlingId);
    }

    private void loggOverlappFOR(LocalDate fom, LocalDate tom, String saksnr, String prefix) {
        LOG.info("FPSAK DETEKTOR PERIODE {} til {}", fom, tom);
        var saker = informasjonssakRepository.finnSakerOpprettetInnenIntervallMedSisteVedtak(fom, tom, saksnr);
        saker.forEach(o -> loggertjeneste.vurderOglagreEventueltOverlapp(prefix, o.getBehandlingId(), o.getAnnenPartAktørId(), o.getTidligsteDato()));
    }

    private void loggOverlappOTH(LocalDate fom, LocalDate tom, String saksnr, String prefix) {
        LOG.info("FPSAK SPBS DETEKTOR PERIODE {} til {}", fom, tom);
        var saker = informasjonssakRepository.finnSakerOpprettetInnenIntervallMedKunUtbetalte(fom, tom, saksnr);
        saker.forEach(o -> syklogger.vurderOglagreEventueltOverlapp(prefix, o.getBehandlingId(), o.getTidligsteDato()));
    }
}
