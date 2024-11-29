package no.nav.foreldrepenger.behandlingsprosess.prosessering;

import java.time.LocalDate;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Grensesnitt for å opprette standardiserte behandlinger
 **/
@ApplicationScoped
public class BehandlingOpprettingTjeneste {
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlendeEnhetTjeneste enhetTjeneste;
    private Historikkinnslag2Repository historikkinnslagRepository;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public BehandlingOpprettingTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            BehandlendeEnhetTjeneste enhetTjeneste,
            Historikkinnslag2Repository historikkinnslagRepository,
            ProsessTaskTjeneste taskTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.enhetTjeneste = enhetTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.taskTjeneste = taskTjeneste;
    }

    public BehandlingOpprettingTjeneste() {
        // CDI
    }

    public Behandling opprettBehandling(Fagsak fagsak, BehandlingType behandlingType) {
        var enhet = enhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return opprettBehandling(fagsak, behandlingType, enhet, BehandlingÅrsakType.UDEFINERT, true);
    }

    public Behandling opprettBehandlingUtenHistorikk(Fagsak fagsak, BehandlingType behandlingType, BehandlingÅrsakType årsak) {
        var enhet = enhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return opprettBehandling(fagsak, behandlingType, enhet, årsak, false);
    }

    public Behandling opprettBehandlingVedKlageinstans(Fagsak fagsak, BehandlingType behandlingType) {
        var enhet = BehandlendeEnhetTjeneste.getKlageInstans();
        return opprettBehandling(fagsak, behandlingType, enhet, BehandlingÅrsakType.UDEFINERT, true);
    }

    public Behandling opprettBehandling(Fagsak fagsak, BehandlingType behandlingType, BehandlingÅrsakType årsak) {
        var enhet = enhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return opprettBehandling(fagsak, behandlingType, enhet, årsak, true);
    }

    public Behandling opprettBehandling(Fagsak fagsak, BehandlingType behandlingType, OrganisasjonsEnhet enhet) {
        return opprettBehandling(fagsak, behandlingType, enhet, BehandlingÅrsakType.UDEFINERT, true);
    }

    public Behandling opprettBehandling(Fagsak fagsak, BehandlingType behandlingType, OrganisasjonsEnhet enhet, BehandlingÅrsakType årsak) {
        return opprettBehandling(fagsak, behandlingType, enhet, årsak, true);
    }

    /**
     * Kjør prosess asynkront (i egen prosess task) videre.
     *
     * @return gruppe assignet til prosess task
     */
    public String asynkStartBehandlingsprosess(Behandling behandling) {
        var taskData = ProsessTaskData.forProsessTask(StartBehandlingTask.class);
        taskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        return taskTjeneste.lagre(taskData);
    }

    private Behandling opprettBehandling(Fagsak fagsak, BehandlingType behandlingType, OrganisasjonsEnhet enhet, BehandlingÅrsakType årsak,
            boolean historikk) {
        var behandling = behandlingskontrollTjeneste.opprettNyBehandling(fagsak, behandlingType,
            beh -> {
                if (!BehandlingÅrsakType.UDEFINERT.equals(årsak)) {
                    BehandlingÅrsak.builder(årsak).buildFor(beh);
                }
                beh.setBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker()));
                beh.setBehandlendeEnhet(enhet);
            });
        // TODO: historikk blir satt til true som ikke har disse behandlingtypene... Da vil man ikke lage historikkinnslag. Bør ryddes opp i.
        if (historikk && Set.of(BehandlingType.ANKE, BehandlingType.INNSYN, BehandlingType.KLAGE).contains(behandlingType)) {
            opprettHistorikkinnslag(behandling, behandlingType);
        }
        return behandling;
    }

    private void opprettHistorikkinnslag(Behandling behandling, BehandlingType behandlingType) {
        var tittel = switch (behandlingType) {
            case ANKE -> "Anke mottatt";
            case INNSYN -> "Innsynsbehandling opprettet";
            case KLAGE -> "Klage mottatt";
            default -> throw new IllegalStateException("Skal ikke lage historikkinnslag for type " + behandlingType);
        };
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(tittel)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

}
