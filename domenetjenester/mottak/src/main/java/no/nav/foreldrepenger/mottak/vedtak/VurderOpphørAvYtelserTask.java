package no.nav.foreldrepenger.mottak.vedtak;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.LoggHistoriskOverlappFPInfotrygdVLTjeneste;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.LoggHistoriskOverlappSYKOMSInfotrygdVLTjeneste;
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
    public static final String HIJACK_FOM_KEY = "fom";
    public static final String HIJACK_TOM_KEY = "tom";

    private InformasjonssakRepository informasjonssakRepository;
    private LoggHistoriskOverlappSYKOMSInfotrygdVLTjeneste syklogger;
    private LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste;



    private VurderOpphørAvYtelser tjeneste;

    VurderOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOpphørAvYtelserTask(VurderOpphørAvYtelser tjeneste,
                                     InformasjonssakRepository informasjonssakRepository,
                                     LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste,
                                     LoggHistoriskOverlappSYKOMSInfotrygdVLTjeneste syklogger) {
        this.tjeneste = tjeneste;
        this.informasjonssakRepository = informasjonssakRepository;
        this.syklogger = syklogger;
        this.loggertjeneste = loggertjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (prosessTaskData.getPropertyValue(HIJACK_KEY_KEY) != null) {
            if (HIJACK_FOR_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(HIJACK_KEY_KEY))) {
                loggOverlappFOR(LocalDate.parse(prosessTaskData.getPropertyValue(HIJACK_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE),
                    LocalDate.parse(prosessTaskData.getPropertyValue(HIJACK_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE));
            } else if (HIJACK_OTH_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(HIJACK_KEY_KEY))) {
                loggOverlappOTH(LocalDate.parse(prosessTaskData.getPropertyValue(HIJACK_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE),
                    LocalDate.parse(prosessTaskData.getPropertyValue(HIJACK_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE));
            }
            return;
        }
        Long behandlingId = prosessTaskData.getBehandlingId();
        tjeneste.vurderOpphørAvYtelser(prosessTaskData.getFagsakId(), behandlingId);
    }

    private void loggOverlappFOR(LocalDate fom, LocalDate tom) {
        LOG.info("FPSAK DETEKTOR PERIODE {} til {}", fom, tom);
        var saker = informasjonssakRepository.finnSakerOpprettetInnenIntervallMedSisteVedtak(fom, tom);
        saker.forEach(o -> loggertjeneste.vurderOglagreEventueltOverlapp(o.getBehandlingId(), o.getAnnenPartAktørId(), o.getTidligsteDato()));
    }

    private void loggOverlappOTH(LocalDate fom, LocalDate tom) {
        LOG.info("FPSAK SPBS DETEKTOR PERIODE {} til {}", fom, tom);
        var saker = informasjonssakRepository.finnSakerOpprettetInnenIntervallMedKunUtbetalte(fom, tom);
        saker.forEach(o -> syklogger.vurderOglagreEventueltOverlapp(o.getBehandlingId(), o.getTidligsteDato()));
    }
}
