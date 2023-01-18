package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.OpprettToTrinnsgrunnlag;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskDataBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@Dependent
@ProsessTask(value = "migrering.rettighet.overstyrt", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MigrerOverstyrtRettTask implements ProsessTaskHandler {

    private InformasjonssakRepository informasjonssakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private UføretrygdRepository uføretrygdRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private OpprettToTrinnsgrunnlag toTrinnsgrunnlag;

    MigrerOverstyrtRettTask() {
        // for CDI proxy
    }

    @Inject
    public MigrerOverstyrtRettTask(InformasjonssakRepository informasjonssakRepository,
                                   YtelsesFordelingRepository ytelsesFordelingRepository,
                                   UføretrygdRepository uføretrygdRepository,
                                   BehandlingRepository behandlingRepository,
                                   OpprettToTrinnsgrunnlag toTrinnsgrunnlag,
                                   ProsessTaskTjeneste taskTjeneste) {
        super();
        this.informasjonssakRepository = informasjonssakRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.uføretrygdRepository = uføretrygdRepository;
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.toTrinnsgrunnlag = toTrinnsgrunnlag;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        var behandlinger = informasjonssakRepository.finnYtelsesfordelingForMigrering();
        if (behandlinger.isEmpty()) {
            return;
        }
        behandlinger.forEach(this::migrer);

        var task = ProsessTaskDataBuilder.forProsessTask(MigrerOverstyrtRettTask.class)
            .medCallId(callId)
            .medPrioritet(100)
            .build();
        taskTjeneste.lagre(task);
    }

    private void migrer(Long behandlingId) {
        // sikre at vi ikke går i beina på noe annet som skjer
        var behandlingLås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId).orElseThrow();
        if (fordelingAggregat.getOverstyrtRettighet().isPresent()) {
            return;
        }
        var uføre = uføretrygdRepository.hentGrunnlag(behandlingId);
        var annenforelderRett = fordelingAggregat.getAnnenForelderRettAvklaring();
        var annenforelderRettEØS = fordelingAggregat.getAnnenForelderRettEØSAvklaring();
        var aleneomsorg = fordelingAggregat.getAleneomsorgAvklaring();
        var uføreAvklaring = uføre.map(UføretrygdGrunnlagEntitet::getUføretrygdOverstyrt).orElse(null);
        var overstyrt = new OppgittRettighetEntitet(annenforelderRett, aleneomsorg, uføreAvklaring, annenforelderRettEØS);
        var builder = ytelsesFordelingRepository.opprettBuilder(behandlingId).medOverstyrtRettighet(overstyrt);
        ytelsesFordelingRepository.lagre(behandlingId, builder.build());
        toTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
    }


}
