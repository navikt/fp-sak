package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.AvsluttetBehandlingStatus;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.BehandlingStatusDto;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.Behandlingsstatus;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.OpprettetBehandlingStatus;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.SakOgBehandlingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.util.env.Cluster;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
@ProsessTask(SakOgBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SakOgBehandlingTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "behandlingskontroll.oppdatersakogbehandling";

    private static final Logger LOG = LoggerFactory.getLogger(SakOgBehandlingTask.class);


    public static final String BEHANDLINGS_TYPE_KODE_KEY = "behandlingsTypeKode";
    public static final String SAKSTEMA_KEY = "sakstemaKode";
    public static final String ANSVARLIG_ENHET_KEY = "ansvarligEnhet";
    public static final String BEHANDLING_STATUS_KEY = "behandlingStatus";
    public static final String BEHANDLING_OPPRETTET_TIDSPUNKT_KEY = "opprettBehandling";
    public static final String BEHANDLINGSTEMAKODE = "behandlingstemakode";

    private SakOgBehandlingTjeneste sakOgBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private boolean brukKafka;
    private boolean erProd;

    SakOgBehandlingTask() {
        //for CDI proxy
    }

    @Inject
    public SakOgBehandlingTask(SakOgBehandlingTjeneste sakOgBehandlingTjeneste,
                               BehandlingRepositoryProvider repositoryProvider) {
        this.sakOgBehandlingTjeneste = sakOgBehandlingTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.brukKafka = !(Cluster.DEV_FSS.equals(Environment.current().getCluster()) && "T4".equalsIgnoreCase(Environment.current().getNamespace().getNamespace()));
        this.erProd = Cluster.PROD_FSS.equals(Environment.current().getCluster());
    }

    public SakOgBehandlingTask(SakOgBehandlingTjeneste sakOgBehandlingTjeneste,
                               BehandlingRepositoryProvider repositoryProvider,
                               boolean brukKafka) {
        this.sakOgBehandlingTjeneste = sakOgBehandlingTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.brukKafka = brukKafka;
        this.erProd = false;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (brukKafka) {
            var behandlingId = prosessTaskData.getBehandlingId();
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            if (Set.of(BehandlingStatus.FATTER_VEDTAK, BehandlingStatus.IVERKSETTER_VEDTAK).contains(behandling.getStatus()))
                return;
            final LocalDateTime tidspunkt;
            if (BehandlingStatus.AVSLUTTET.equals(behandling.getStatus())) {
                tidspunkt = behandling.getAvsluttetDato();
            } else {
                tidspunkt = BehandlingStatus.OPPRETTET.equals(behandling.getStatus()) ? behandling.getOpprettetDato() : LocalDateTime.now();
            }
            var dto = BehandlingStatusDto.getBuilder()
                .medAktørId(behandling.getAktørId())
                .medBehandlingId(behandlingId)
                .medSaksnummer(behandling.getFagsak().getSaksnummer())
                .medBehandlingStatus(behandling.getStatus())
                .medBehandlingType(behandling.getType())
                .medBehandlingTema(BehandlingTema.fraFagsak(behandling.getFagsak(), familieHendelseRepository
                    .hentAggregatHvisEksisterer(behandlingId).map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null)))
                .medEnhet(behandling.getBehandlendeOrganisasjonsEnhet())
                .medHendelsesTidspunkt(tidspunkt)
                .build();
            try {
                sakOgBehandlingTjeneste.behandlingStatusEndret(dto);
                return;
            } catch (Exception e) {
                LOG.info("SOBKAFKA noe gikk feil for behandling {}", behandlingId, e);
                if (erProd)
                    throw e;
            }
        }

        String behandlingStatusKode = prosessTaskData.getPropertyValue(BEHANDLING_STATUS_KEY);
        if (BehandlingStatus.AVSLUTTET.getKode().equals(behandlingStatusKode)) {
            AvsluttetBehandlingStatus avsluttetBehandlingStatus = new AvsluttetBehandlingStatus();
            fyllUtBehandlingsInfo(prosessTaskData, avsluttetBehandlingStatus);
            avsluttetBehandlingStatus.setAvslutningsStatus(BehandlingStatus.AVSLUTTET.getKode());
            sakOgBehandlingTjeneste.behandlingAvsluttet(avsluttetBehandlingStatus);
        } else {
            OpprettetBehandlingStatus opprettetBehandlingStatus = new OpprettetBehandlingStatus();
            opprettetBehandlingStatus.setHendelsesTidspunkt(LocalDate.parse(prosessTaskData.getPropertyValue(BEHANDLING_OPPRETTET_TIDSPUNKT_KEY)));
            opprettetBehandlingStatus.setBehandlingsTemaKode(prosessTaskData.getPropertyValue(BEHANDLINGSTEMAKODE));
            fyllUtBehandlingsInfo(prosessTaskData, opprettetBehandlingStatus);
            sakOgBehandlingTjeneste.behandlingOpprettet(opprettetBehandlingStatus);
        }
    }

    private void fyllUtBehandlingsInfo(ProsessTaskData prosessTaskData, Behandlingsstatus behandlingsstatus) {
        behandlingsstatus.setAktørId(String.valueOf(prosessTaskData.getAktørId()));
        behandlingsstatus.setBehandlingsId(String.valueOf(prosessTaskData.getBehandlingId()));
        behandlingsstatus.setBehandlingsTypeKode(prosessTaskData.getPropertyValue(BEHANDLINGS_TYPE_KODE_KEY));
        behandlingsstatus.setSakstemaKode(prosessTaskData.getPropertyValue(SAKSTEMA_KEY));
        behandlingsstatus.setAnsvarligEnhetRef(prosessTaskData.getPropertyValue(ANSVARLIG_ENHET_KEY));
    }
}
