package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import org.slf4j.Logger;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.vedtak.intern.AutomatiskFagsakAvslutningTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskFagsakAvslutningTjeneste {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AutomatiskFagsakAvslutningTjeneste.class);

    private ProsessTaskRepository prosessTaskRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRepository fagsakRepository;

    AutomatiskFagsakAvslutningTjeneste() {
        // For CDI?
    }

    @Inject
    public AutomatiskFagsakAvslutningTjeneste(ProsessTaskRepository prosessTaskRepository,
                                              FagsakRelasjonRepository fagsakRelasjonRepository,
                                              FagsakRepository fagsakRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.fagsakRepository = fagsakRepository;
    }

    String avsluttFagsaker(String batchname, LocalDate date, int antDager) {
        String resultat = avsluttFPFagsaker(batchname, date, antDager);
        avsluttSVPFagsaker();
        return resultat;
    }

    String avsluttFPFagsaker(String batchname, LocalDate date, int antDager) {
        List<FagsakRelasjon> fagsakRelasjons = fagsakRelasjonRepository.finnRelasjonerForAvsluttningAvFagsaker(date,antDager);

        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        for (FagsakRelasjon fagsakRelasjon : fagsakRelasjons) {
            List<ProsessTaskData> tasks = new ArrayList<>();
            if (fagsakRelasjon.getFagsakNrEn().getStatus().getKode().equals(FagsakStatus.LØPENDE.getKode())) {
                String nyCallId = callId + fagsakRelasjon.getFagsakNrEn().getId();
                log.info("{} oppretter task med ny callId: {} ", getClass().getSimpleName(), nyCallId);
                tasks.add(opprettFagsakAvslutningTask(fagsakRelasjon.getFagsakNrEn(), nyCallId));
            }
            if (fagsakRelasjon.getFagsakNrTo().isPresent()
                && fagsakRelasjon.getFagsakNrTo().get().getStatus().getKode().equals(FagsakStatus.LØPENDE.getKode())) {
                String nyCallId = callId + fagsakRelasjon.getFagsakNrTo().get().getId();
                log.info("{} oppretter task med ny callId: {} ", getClass().getSimpleName(), nyCallId);
                tasks.add(opprettFagsakAvslutningTask(fagsakRelasjon.getFagsakNrTo().get(), nyCallId));
            }
            if (!tasks.isEmpty()) {
                tasks.forEach(t -> t.setPrioritet(100));
                ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
                tasks.forEach(gruppe::addNesteSekvensiell);
                prosessTaskRepository.lagre(gruppe);
            }
        }
        return batchname + "-" + (UUID.randomUUID().toString());
    }

    void avsluttSVPFagsaker() {
        List<Fagsak> fagsaker = fagsakRepository.hentForStatusOgYtelseType(FagsakStatus.LØPENDE, FagsakYtelseType.SVANGERSKAPSPENGER);

        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        for (Fagsak fagsak : fagsaker) {
            List<ProsessTaskData> tasks = new ArrayList<>();
            String nyCallId = callId + fagsak.getId();
            log.info("{} oppretter task med ny callId: {} ", getClass().getSimpleName(), nyCallId);
            tasks.add(opprettFagsakAvslutningTask(fagsak, nyCallId));

            if (!tasks.isEmpty()) {
                tasks.forEach(t -> t.setPrioritet(100));
                ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
                tasks.forEach(gruppe::addNesteSekvensiell);
                prosessTaskRepository.lagre(gruppe);
            }
        }
    }

    private ProsessTaskData opprettFagsakAvslutningTask(Fagsak fagsak, String callId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskFagsakAvslutningTask.TASKTYPE);
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        prosessTaskData.setPrioritet(100);
        // unik per task da det er ulike tasks for hver behandling
        prosessTaskData.setCallId(callId);
        return prosessTaskData;
    }

    public List<TaskStatus> hentStatusForFagsakAvslutningGruppe(String gruppe) {
        return prosessTaskRepository.finnStatusForTaskIGruppe(AutomatiskFagsakAvslutningTask.TASKTYPE, gruppe);
    }
}
