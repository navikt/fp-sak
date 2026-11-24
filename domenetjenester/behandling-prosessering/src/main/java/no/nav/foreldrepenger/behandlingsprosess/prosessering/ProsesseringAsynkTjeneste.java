package no.nav.foreldrepenger.behandlingsprosess.prosessering;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class ProsesseringAsynkTjeneste {

    private ProsessTaskTjeneste taskTjeneste;

    ProsesseringAsynkTjeneste() {
        // For CDI proxy
    }

    @Inject
    public ProsesseringAsynkTjeneste(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    public Map<String, ProsessTaskData> sjekkProsessTaskPågår(Saksnummer saksnummer, Long behandlingId, String gruppe) {
        var statusProsessTasks = sjekkStatusProsessTasksGrouped(saksnummer, behandlingId, gruppe);

        var nestePerGruppe = nesteProsessTaskPerGruppe(statusProsessTasks);

        if (angittGruppeErFerdig(gruppe, nestePerGruppe)) {
            nestePerGruppe = nesteProsessTaskPerGruppe(sjekkStatusProsessTasksGrouped(saksnummer, behandlingId, null));
        }

        return nestePerGruppe;
    }

    /**
     * Sjekker om prosess tasks pågår nå for angitt behandling. Returnerer neste
     * utestående tasks per gruppe for status (KLAR, FEILET, VENTER), men ikke
     * FERDIG, SUSPENDERT (unntatt der matcher angitt gruppe)
     *
     * Hvis gruppe angis sjekkes kun angitt gruppe. Dersom denne er null returneres
     * status for alle åpne grupper (ikke-ferdig) for angitt behandling. Tasks som
     * er {@link ProsessTaskStatus#FERDIG} ignoreres i resultatet når gruppe ikke er
     * angitt.
     */
    public Map<String, ProsessTaskData> sjekkProsessTaskPågårForBehandling(Behandling behandling, String gruppe) {
        return sjekkProsessTaskPågår(behandling.getSaksnummer(), behandling.getId(), gruppe);
    }

    /**
     * Merge ny gruppe med eksisterende, hvis tidligere gruppe er i gang ignoreres
     * input gruppe her. Hvis tidligere gruppe har feil, overskrives denne (ny
     * skrives, gamle fjernes). For å merge sees det på individuelle tasks inne i
     * gruppen (da gruppe id kan være forskjellig uansett).
     */
    public String lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(Saksnummer saksnummer, Long behandlingId, ProsessTaskGruppe gruppe) {
        // oppretter nye tasks hvis gamle har feilet og matcher angitt gruppe, eller tidligere er FERDIG. Ignorerer hvis tidligere gruppe fortsatt
        // er KLAR
        var nyeTasks = gruppe.getTasks();
        var eksisterendeTasks = sjekkStatusProsessTasks(saksnummer, behandlingId, null);

        if (eksisterendeTasks.isEmpty()) {
            // legg inn nye
            return taskTjeneste.lagre(gruppe);
        }

        // hvis noen er FEILET så oppretter vi ikke ny
        var feilet = eksisterendeTasks.stream().filter(t -> t.getStatus().equals(ProsessTaskStatus.FEILET)).findFirst();

        if (feilet.isEmpty()) {
            var nyeTaskTyper = nyeTasks.stream().map(t -> t.task().taskType()).collect(Collectors.toSet());
            var eksisterendeTaskTyper = eksisterendeTasks.stream().map(ProsessTaskData::taskType).collect(Collectors.toSet());
            if (eksisterendeTaskTyper.containsAll(nyeTaskTyper)){
                return eksisterendeTasks.getFirst().getGruppe();
            }
            return taskTjeneste.lagre(gruppe);
        }
        return feilet.get().getGruppe();

    }

    private Map<String, ProsessTaskData> nesteProsessTaskPerGruppe(Map<String, List<ProsessTaskData>> tasks) {
        // velg top task per gruppe

        return tasks.values().stream()
                .filter(prosessTaskData -> !prosessTaskData.isEmpty())
                .map(/*
                 * NB: avhenger av enum ordinal!
                 */prosessTaskData -> prosessTaskData
                        .stream().min(Comparator.comparing(ProsessTaskData::getSekvens)
                                .thenComparing(Comparator.comparing(ProsessTaskData::getStatus).reversed())).get())
                .collect(Collectors.toMap(ProsessTaskData::getGruppe, Function.identity()));
    }

    private boolean angittGruppeErFerdig(String gruppe, Map<String, ProsessTaskData> nestePerGruppe) {
        return gruppe != null && (nestePerGruppe.isEmpty() || nestePerGruppe.size() == 1 && nestePerGruppe.containsKey(gruppe) && nestePerGruppe.get(
            gruppe).getStatus().erKjørt());
    }

    private Map<String, List<ProsessTaskData>> sjekkStatusProsessTasksGrouped(Saksnummer saksnummer, Long behandlingId, String gruppe) {
        var tasks = sjekkStatusProsessTasks(saksnummer, behandlingId, gruppe);
        return tasks.stream().collect(Collectors.groupingBy(ProsessTaskData::getGruppe));
    }


    private List<ProsessTaskData> sjekkStatusProsessTasks(Saksnummer saksnummer, Long behandlingId, String gruppe) {
        Objects.requireNonNull(saksnummer, "saksnummer");

        var now = LocalDateTime.now().withNano(0).withSecond(0);

        // et tidsrom for neste kjøring vi kan ta hensyn til. Det som er lenger ut i fremtiden er ikke relevant her, kun det vi kan forvente
        // kjøres i umiddelbar fremtid. tar i tillegg hensyn til alt som skulle ha vært kjørt tilbake i tid (som har stoppet av en eller annen
        // grunn).
        var fom = now.minusWeeks(2);
        var tom = now.plusMinutes(10); // kun det som forventes kjørt om kort tid.

        if (gruppe != null) {
            var aktuelle = taskTjeneste.finnUferdigForGruppe(gruppe).stream()
                .filter(t -> Objects.equals(t.getSaksnummer(), saksnummer.getVerdi()))
                .filter(t -> behandlingId == null || Objects.equals(t.getBehandlingIdAsLong(), behandlingId))
                .filter(t -> t.getNesteKjøringEtter() == null || (t.getNesteKjøringEtter().isAfter(fom) && t.getNesteKjøringEtter().isBefore(tom)))
                .toList();
            if (!aktuelle.isEmpty()) {
                return aktuelle;
            }
        }

        return taskTjeneste.finnAktive(saksnummer.getVerdi()).stream()
            .filter(t -> behandlingId == null || Objects.equals(t.getBehandlingIdAsLong(), behandlingId))
            .filter(t -> t.getNesteKjøringEtter() == null || (t.getNesteKjøringEtter().isAfter(fom) && t.getNesteKjøringEtter().isBefore(tom)))
            .toList();
    }

}
