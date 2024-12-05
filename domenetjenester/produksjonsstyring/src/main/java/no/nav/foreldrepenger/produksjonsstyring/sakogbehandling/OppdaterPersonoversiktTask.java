package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
@ProsessTask(value = "oppgavebehandling.oppdaterpersonoversikt", prioritet = 3)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OppdaterPersonoversiktTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(OppdaterPersonoversiktTask.class);

    public static String PH_REF_KEY = "behandlingRef";
    public static String PH_STATUS_KEY = "status";
    public static String PH_TID_KEY = "tid";
    public static String PH_TYPE_KEY = "type";


    private PersonoversiktHendelseProducer hendelseProducer;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersoninfoAdapter personinfoAdapter;
    private BehandlendeEnhetTjeneste enhetTjeneste;


    OppdaterPersonoversiktTask() {
        //for CDI proxy
    }

    @Inject
    public OppdaterPersonoversiktTask(PersonoversiktHendelseProducer hendelseProducer,
                                      PersoninfoAdapter personinfoAdapter,
                                      BehandlingRepositoryProvider repositoryProvider,
                                      BehandlendeEnhetTjeneste enhetTjeneste) {
        super();
        this.hendelseProducer = hendelseProducer;
        this.personinfoAdapter = personinfoAdapter;
        this.enhetTjeneste = enhetTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingId != null ? behandlingRepository.hentBehandlingReadOnly(behandlingId) :
            behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElse(null);
        if (behandling == null) {
            return;
        }
        var behandlingRef = prosessTaskData.getPropertyValue(PH_REF_KEY);
        var behandlingStatus = BehandlingStatus.fraKode(prosessTaskData.getPropertyValue(PH_STATUS_KEY));
        var behandlingType = BehandlingType.fraKode(prosessTaskData.getPropertyValue(PH_TYPE_KEY));
        var tidspunkt = LocalDateTime.parse(prosessTaskData.getPropertyValue(PH_TID_KEY), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        var enhet = behandlingId != null ? behandling.getBehandlendeEnhet() : getBehandlendeEnhetForSak(behandling);
        var behandlingTema = BehandlingTema.fraFagsak(behandling.getFagsak(), familieHendelseRepository
            .hentAggregatHvisEksisterer(behandling.getId()).map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null));
        var erAvsluttet = BehandlingStatus.AVSLUTTET.equals(behandlingStatus);
        var hendelseType = erAvsluttet ? "behandlingAvsluttet" : "behandlingOpprettet";

        var callId = Optional.ofNullable(MDCOperations.getCallId()).orElseGet(MDCOperations::generateCallId);

        LOG.info("OppdaterPersonoversikt sender behandlingsstatus {} for id {}", behandlingStatus.getKode(), behandlingRef);

        var ident = personinfoAdapter.hentFnr(behandling.getAktørId()).orElse(null);
        var personSoB = PersonoversiktBehandlingStatusDto.lagPersonoversiktBehandlingStatusDto(hendelseType, callId, behandling.getAktørId(),
            tidspunkt, behandlingType, behandlingRef, behandlingTema, enhet, ident, erAvsluttet);
        hendelseProducer.sendJsonMedNøkkel(createUniqueKey(String.valueOf(behandling.getId()), behandling.getStatus().getKode()), StandardJsonConfig.toJson(personSoB));
    }

    private String createUniqueKey(String behandlingsId, String event) {
        return String.format("%s_%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandlingsId, event);
    }

    private String getBehandlendeEnhetForSak(Behandling behandling) {
        return enhetTjeneste.finnBehandlendeEnhetFra(behandling).enhetId();
    }

}
