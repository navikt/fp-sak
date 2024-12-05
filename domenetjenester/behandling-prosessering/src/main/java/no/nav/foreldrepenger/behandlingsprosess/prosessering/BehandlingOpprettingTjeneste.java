package no.nav.foreldrepenger.behandlingsprosess.prosessering;

import java.time.LocalDate;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Grensesnitt for å opprette standardiserte behandlinger
 **/
@ApplicationScoped
public class BehandlingOpprettingTjeneste {

    private static final Map<BehandlingType, HistorikkinnslagType> BEHANDLING_HISTORIKK = Map.ofEntries(
            Map.entry(BehandlingType.ANKE, HistorikkinnslagType.ANKEBEH_STARTET),
            Map.entry(BehandlingType.INNSYN, HistorikkinnslagType.INNSYN_OPPR),
            Map.entry(BehandlingType.KLAGE, HistorikkinnslagType.KLAGEBEH_STARTET));

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlendeEnhetTjeneste enhetTjeneste;
    private HistorikkRepository historikkRepository;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public BehandlingOpprettingTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            BehandlendeEnhetTjeneste enhetTjeneste,
            HistorikkRepository historikkRepository,
            ProsessTaskTjeneste taskTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.enhetTjeneste = enhetTjeneste;
        this.historikkRepository = historikkRepository;
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
        if (historikk) {
            opprettHistorikkinnslag(behandling, behandlingType);
        }
        return behandling;
    }

    private void opprettHistorikkinnslag(Behandling behandling, BehandlingType behandlingType) {
        var type = BEHANDLING_HISTORIKK.get(behandlingType);
        if (type == null) {
            return;
        }
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SØKER);
        historikkinnslag.setType(type);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());

        new HistorikkInnslagTekstBuilder().medHendelse(type)
                .medBegrunnelse(type.getNavn())
                .build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

}
