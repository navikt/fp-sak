package no.nav.foreldrepenger.web.app.tjenester.abakus;

import java.util.Properties;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.arbeidsforhold.RegisterdataCallback;
import no.nav.foreldrepenger.domene.registerinnhenting.task.InnhentIAYIAbakusTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@SuppressWarnings("unused")
@Dependent
public class IAYRegisterdataTjeneste {

    private static final TaskType ABAKUS_TASK = TaskType.forProsessTask(InnhentIAYIAbakusTask.class);

    private static final Logger LOG = LoggerFactory.getLogger(IAYRegisterdataTjeneste.class);

    private ProsessTaskTjeneste taskTjeneste;

    public IAYRegisterdataTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    /**
     * Standard ctor som injectes av CDI.
     */
    @Inject
    public IAYRegisterdataTjeneste(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    public void håndterCallback(RegisterdataCallback callback) {
        LOG.info("Mottatt callback fra Abakus etter registerinnhenting for behandlingId={}, eksisterendeGrunnlag={}, nyttGrunnlag={}",
            callback.behandlingId(), callback.eksisterendeGrunnlagRef(), callback.oppdatertGrunnlagRef());

        var tasksSomVenterPåSvar = taskTjeneste.finnAlle(ProsessTaskStatus.VENTER_SVAR)
            .stream()
            .filter(it -> ABAKUS_TASK.equals(it.taskType()))
            .filter(it -> it.getBehandlingIdAsLong().equals(callback.behandlingId()))
            .toList();

        if (tasksSomVenterPåSvar.isEmpty()) {
            LOG.info("Mottatt callback hvor ingen task venter på svar... {}", callback);
        }
        tasksSomVenterPåSvar.forEach(t -> mottaHendelse(t, callback.oppdatertGrunnlagRef()));
    }

    private void mottaHendelse(ProsessTaskData task, UUID oppdatertGrunnlagRef) {
        var props = new Properties();
        props.setProperty(InnhentIAYIAbakusTask.OPPDATERT_GRUNNLAG_KEY, oppdatertGrunnlagRef.toString());
        taskTjeneste.mottaHendelse(task, InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK, props);
        LOG.info("Behandler hendelse {} i task {}, behandling id {}", InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK, task.getId(), task.getBehandlingIdAsLong());
    }
}
