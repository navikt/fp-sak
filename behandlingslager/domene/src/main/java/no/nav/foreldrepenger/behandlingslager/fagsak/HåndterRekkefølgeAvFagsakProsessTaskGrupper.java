package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskLifecycleObserver;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskVeto;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskHandlerRef;

/**
 * Vetoer kjøring av prosesstasks som tilhører grupper som er senere enn tidligste prosesstaskgruppe for en fagsak.
 *
 * Denne plugges automatisk inn i prosesstask rammeverket (vha. CDI og {@link ProsessTaskLifecycleObserver} interfacet) og kan veto en
 * kjøring av en ProsessTask (denne vil da forsøkes kjøres om igjen om ca. 30 sek default).
 */
@ApplicationScoped
public class HåndterRekkefølgeAvFagsakProsessTaskGrupper implements ProsessTaskLifecycleObserver {
    private static final Logger LOG = LoggerFactory.getLogger(HåndterRekkefølgeAvFagsakProsessTaskGrupper.class);
    private FagsakProsessTaskRepository repository;
    private ProsessTaskTjeneste taskTjeneste;

    public HåndterRekkefølgeAvFagsakProsessTaskGrupper() {
        // for CDI proxy
    }

    @Inject
    public HåndterRekkefølgeAvFagsakProsessTaskGrupper(FagsakProsessTaskRepository repository, ProsessTaskTjeneste taskTjeneste) {
        this.repository = repository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public ProsessTaskVeto vetoKjøring(ProsessTaskData ptData) {
        var fagsakId = ptData.getFagsakId();
        if (fagsakId == null) {
            return new ProsessTaskVeto(false, ptData.getId()); // do nothing, er ikke relatert til fagsak/behandling
        }

        var blokkerendeTask = repository.sjekkTillattKjøreFagsakProsessTask(ptData);
        // dersom blokkerende task er tom, vetoes ikke tasken
        var vetoed = blokkerendeTask.isPresent();
        if (vetoed) {
            var blokker = taskTjeneste.finn(blokkerendeTask.get().getProsessTaskId());
            LOG.info("Vetoer kjøring av prosesstask[{}] av {} for fagsak [{}] , er blokkert av prosesstask[{}] av {} for samme fagsak.",
                ptData.getId(), ptData.taskType(), ptData.getFagsakId(), blokker.getId(), blokker.taskType());

            return new ProsessTaskVeto(false, ptData.getId(), blokker.getId(), getClass().getSimpleName()
                + " vetoer pga definert rekkefølge i FAGSAK_PROSESS_TASK.GRUPPE_SEKVENSNR. Blir pukket når blokkerende task kjøres FERDIG.");
        }

        return new ProsessTaskVeto(false, ptData.getId()); // do nothing, er ikke relatert til fagsak/behandling
    }

    /** Denne metoden kalles umiddelbart etter at prosesstasks er oppretttet. En gruppe kan også bestå av 1 enkel task. */
    @Override
    public void opprettetProsessTaskGruppe(ProsessTaskGruppe gruppe) {

        var gruppeSekvensNr = getGruppeSekvensNr();

        for (var entry : gruppe.getTasks()) {

            var task = entry.task();
            if (task.getFagsakId() == null) {
                // ikke interessant her, move along
                continue;
            }

            try (var handler = LocalProsessTaskHandlerRef.lookup(task.taskType())) {
                var rekkefølge = handler.getFagsakProsesstaskRekkefølge();
                var sekvensNr = rekkefølge.gruppeSekvens() ? gruppeSekvensNr : null;
                repository.lagre(new FagsakProsessTask(task.getFagsakId(), task.getId(), task.getBehandlingIdAsLong(), sekvensNr));
            }
        }
    }

    /**
     * Rekkefølge av grupper. Bruker tidsstempel for enkelt skyld inntil videre.
     * Ellers må vi ha bokholderi på sekvens for en gruppe på en gitt fagsak dersom det skal være absolutt mulig å opprette grupper i rekkefølge
     * på samme fagsak i samme millisek.
     */
    protected Long getGruppeSekvensNr() {
        return Instant.now().toEpochMilli();
    }

    private static class LocalProsessTaskHandlerRef extends ProsessTaskHandlerRef {

        private LocalProsessTaskHandlerRef(ProsessTaskHandler bean) {
            super(bean);
        }

        private FagsakProsesstaskRekkefølge getFagsakProsesstaskRekkefølge() {
            var clazz = getTargetClassExpectingAnnotation(FagsakProsesstaskRekkefølge.class);
            if (!clazz.isAnnotationPresent(FagsakProsesstaskRekkefølge.class)) {
                throw new UnsupportedOperationException(clazz.getSimpleName() + " må være annotert med "
                    + FagsakProsesstaskRekkefølge.class.getSimpleName() + " for å kobles til en Fagsak");
            }
            return clazz.getAnnotation(FagsakProsesstaskRekkefølge.class);
        }

        public static LocalProsessTaskHandlerRef lookup(TaskType taskType) {
            var bean = lookupHandler(taskType);
            return new LocalProsessTaskHandlerRef(bean);
        }

    }

}
