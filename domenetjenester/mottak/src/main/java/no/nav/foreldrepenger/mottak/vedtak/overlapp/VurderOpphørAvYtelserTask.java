package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(VurderOpphørAvYtelserTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOpphørAvYtelserTask extends GenerellProsessTask {
    public static final String TASKTYPE = "iverksetteVedtak.vurderOpphørAvYtelser";

    public static final String K9_YTELSE_KEY = "k9ytelse";
    public static final String K9_INNLEGGELSE_KEY = "k9innleggelse";
    public static final String K9_SAK_KEY = "k9sak";
    public static final String K9_REVURDER_KEY = "k9revurder";

    private LoggOverlappEksterneYtelserTjeneste overlappsLoggerTjeneste;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    private VurderOpphørAvYtelser vurderOpphørAvYtelser;
    private HåndterOpphørAvYtelser håndterOpphørAvYtelser;

    VurderOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOpphørAvYtelserTask(VurderOpphørAvYtelser vurderOpphørAvYtelser,
                                     HåndterOpphørAvYtelser håndterOpphørAvYtelser,
                                     LoggOverlappEksterneYtelserTjeneste overlappsLoggerTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider) {
        super();
        this.vurderOpphørAvYtelser = vurderOpphørAvYtelser;
        this.håndterOpphørAvYtelser = håndterOpphørAvYtelser;
        this.overlappsLoggerTjeneste = overlappsLoggerTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        // Placeholder for å opprettet VKY når det kommer inn overlappende vedtak fra K9sak, VLSP, mfl
        if (prosessTaskData.getPropertyValue(K9_REVURDER_KEY) != null) {
            //TODO TFP-4475: Flytte dette til HåndterOpphørAvYtelserTask
            var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
            var saksnr = prosessTaskData.getPropertyValue(K9_SAK_KEY);
            var ytelse = prosessTaskData.getPropertyValue(K9_YTELSE_KEY);
            var intro = prosessTaskData.getPropertyValue(K9_INNLEGGELSE_KEY) != null ? "(Innlagt) saksnr" : "saksnr";
            var beskrivelse = String.format("%s %s %s overlapper %s saksnr %s", ytelse, intro, saksnr, fagsak.getYtelseType().getNavn(), fagsak.getSaksnummer().getVerdi());

            håndterOpphørAvYtelser.oppdaterEllerOpprettRevurdering(fagsak, beskrivelse, BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER);
            return;
        }
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        //kjøres for førstegangsvedtak og revurderingsvedtak for fp og SVP
        overlappsLoggerTjeneste.loggOverlappForVedtakFPSAK(behandlingId, behandling.getFagsak().getSaksnummer(), behandling.getAktørId());
        //kjøres kun for førstegangsvedtak for svp og fp
        if (!behandling.erRevurdering()) {
            vurderOpphørAvYtelser.vurderOpphørAvYtelser(prosessTaskData.getFagsakId(), behandlingId);
        }
    }

}
