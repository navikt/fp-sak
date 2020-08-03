package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(VurderOpphørAvYtelserTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOpphørAvYtelserTask extends GenerellProsessTask {
    public static final String TASKTYPE = "iverksetteVedtak.vurderOpphørAvYtelser";

    private static final Logger LOG = LoggerFactory.getLogger(VurderOpphørAvYtelserTask.class);
    public static final String K9_YTELSE_KEY = "k9ytelse";
    public static final String K9_SAK_KEY = "k9sak";
    public static final String K9_FOM_KEY = "k9fom";
    public static final String K9_TOM_KEY = "k9tom";

    private IdentifiserOverlappendeInfotrygdYtelseTjeneste overlappsLoggerTjeneste;
    private BehandlingRepository behandlingRepository;


    private VurderOpphørAvYtelser tjeneste;

    VurderOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOpphørAvYtelserTask(VurderOpphørAvYtelser tjeneste,
                                     IdentifiserOverlappendeInfotrygdYtelseTjeneste overlappsLoggerTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider) {
        super();
        this.tjeneste = tjeneste;
        this.overlappsLoggerTjeneste = overlappsLoggerTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();

    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        // Placeholder for å opprettet VKY når det kommer inn overlappende vedtak fra K9sak, VLSP, mfl
        if (prosessTaskData.getPropertyValue(K9_SAK_KEY) != null) {
            String saksnr = prosessTaskData.getPropertyValue(K9_SAK_KEY);
            String ytelse = prosessTaskData.getPropertyValue(K9_YTELSE_KEY);
            LocalDate fom = LocalDate.parse(prosessTaskData.getPropertyValue(K9_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate tom = LocalDate.parse(prosessTaskData.getPropertyValue(K9_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
            LOG.info("Vedtatt-Ytelse task skal opprette VKY for {} {} {} {}", ytelse, saksnr, fom, tom);
            return;
        }
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        //kjøres for førstegangsvedtak og revurderingsvedtak for fp og SVP
        overlappsLoggerTjeneste.vurderOglagreEventueltOverlapp(behandlingId);
        //kjøres kun for førstegangsvedtak for svp og fp
        if (!behandling.erRevurdering()) {
            tjeneste.vurderOpphørAvYtelser(prosessTaskData.getFagsakId(), behandlingId);
        }
    }

}
