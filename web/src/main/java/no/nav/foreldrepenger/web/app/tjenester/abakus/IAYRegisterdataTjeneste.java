package no.nav.foreldrepenger.web.app.tjenester.abakus;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.RegisterdataCallback;
import no.nav.foreldrepenger.domene.registerinnhenting.task.InnhentIAYIAbakusTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelseMottak;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

@SuppressWarnings("unused")
@Dependent
public class IAYRegisterdataTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(IAYRegisterdataTjeneste.class);

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private ProsessTaskHendelseMottak hendelseMottak;

    public IAYRegisterdataTjeneste() {
    }

    /**
     * Standard ctor som injectes av CDI.
     */
    @Inject
    public IAYRegisterdataTjeneste(InntektArbeidYtelseTjeneste iayTjeneste, ProsessTaskRepository prosessTaskRepository,
                                   ProsessTaskHendelseMottak hendelseMottak) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.prosessTaskRepository = prosessTaskRepository;
        this.hendelseMottak = hendelseMottak;
    }

    public void håndterCallback(RegisterdataCallback callback) {
        LOG.info("Mottatt callback fra Abakus etter registerinnhenting for behandlingId={}, eksisterendeGrunnlag={}, nyttGrunnlag={}",
            callback.getBehandlingId(), callback.getEksisterendeGrunnlagRef(), callback.getOppdatertGrunnlagRef());
        final var tasksSomVenterPåSvar = prosessTaskRepository.finnUferdigeBatchTasks(InnhentIAYIAbakusTask.TASKTYPE)
            .stream()
            .filter(it -> it.getBehandlingId().equals("" + callback.getBehandlingId()))
            .filter(it -> ProsessTaskStatus.VENTER_SVAR.equals(it.getStatus()))
            .collect(Collectors.toList());

        if (tasksSomVenterPåSvar.size() == 1) {
            mottaHendelse(tasksSomVenterPåSvar.get(0), callback.getOppdatertGrunnlagRef());
        } else if (tasksSomVenterPåSvar.isEmpty()) {
            LOG.info("Mottatt callback hvor ingen task venter på svar... {}", callback);
        } else {
            LOG.info("Mottatt callback som svarer til flere tasks som venter. callback={}, tasks={}", callback, tasksSomVenterPåSvar);
        }
    }

    private void mottaHendelse(ProsessTaskData task, UUID oppdatertGrunnlagRef) {
        Objects.requireNonNull(task, "Task");
        var venterHendelse = Optional.ofNullable(task.getPropertyValue(ProsessTaskData.HENDELSE_PROPERTY));
        if (!Objects.equals(ProsessTaskStatus.VENTER_SVAR, task.getStatus()) || venterHendelse.isEmpty()) {
            throw new IllegalStateException("Uventet hendelse " + InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK + " mottatt i tilstand " + task.getStatus());
        }
        if (!Objects.equals(venterHendelse.get(), InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK)) {
            throw new IllegalStateException("Uventet hendelse " + InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK + " mottatt, venter hendelse " + venterHendelse.get());
        }
        task.setStatus(ProsessTaskStatus.KLAR);
        task.setNesteKjøringEtter(LocalDateTime.now());
        task.setProperty(InnhentIAYIAbakusTask.OPPDATERT_GRUNNLAG_KEY, oppdatertGrunnlagRef.toString());
        LOG.info("Behandler hendelse {} i task {}, behandling id {}", InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK, task.getId(), task.getBehandlingId()); //$NON-NLS-1$
        prosessTaskRepository.lagre(task);
    }
}
