package no.nav.foreldrepenger.web.app.tjenester.abakus;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.RegisterdataCallback;
import no.nav.foreldrepenger.domene.registerinnhenting.task.InnhentIAYIAbakusTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

@SuppressWarnings("unused")
@Dependent
public class IAYRegisterdataTjeneste {

    private static final TaskType ABAKUS_TASK = TaskType.forProsessTask(InnhentIAYIAbakusTask.class);

    private static final Logger LOG = LoggerFactory.getLogger(IAYRegisterdataTjeneste.class);

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ProsessTaskTjeneste taskTjeneste;

    public IAYRegisterdataTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    /**
     * Standard ctor som injectes av CDI.
     */
    @Inject
    public IAYRegisterdataTjeneste(InntektArbeidYtelseTjeneste iayTjeneste, ProsessTaskTjeneste taskTjeneste) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.taskTjeneste = taskTjeneste;
    }

    public void håndterCallback(RegisterdataCallback callback) {
        LOG.info("Mottatt callback fra Abakus etter registerinnhenting for behandlingId={}, eksisterendeGrunnlag={}, nyttGrunnlag={}",
            callback.getBehandlingId(), callback.getEksisterendeGrunnlagRef(), callback.getOppdatertGrunnlagRef());

        var tasksSomVenterPåSvar = taskTjeneste.finnAlle(ProsessTaskStatus.VENTER_SVAR)
            .stream()
            .filter(it -> ABAKUS_TASK.equals(it.taskType()))
            .filter(it -> it.getBehandlingId().equals("" + callback.getBehandlingId()))
            .toList();

        if (tasksSomVenterPåSvar.isEmpty()) {
            LOG.info("Mottatt callback hvor ingen task venter på svar... {}", callback);
        }
        tasksSomVenterPåSvar.forEach(t -> mottaHendelse(t, callback.getOppdatertGrunnlagRef()));
        /*
        if (tasksSomVenterPåSvar.size() == 1) {
            mottaHendelse(tasksSomVenterPåSvar.get(0), callback.getOppdatertGrunnlagRef());
        } else if (tasksSomVenterPåSvar.isEmpty()) {
            LOG.info("Mottatt callback hvor ingen task venter på svar... {}", callback);
        } else {
            LOG.info("Mottatt callback som svarer til flere tasks som venter. callback={}, tasks={}", callback, tasksSomVenterPåSvar);
        }
        */

    }

    private void mottaHendelse(ProsessTaskData task, UUID oppdatertGrunnlagRef) {
        var props = new Properties();
        props.setProperty(InnhentIAYIAbakusTask.OPPDATERT_GRUNNLAG_KEY, oppdatertGrunnlagRef.toString());
        taskTjeneste.mottaHendelse(task, InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK, props);
        LOG.info("Behandler hendelse {} i task {}, behandling id {}", InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK, task.getId(), task.getBehandlingId());
    }
}
