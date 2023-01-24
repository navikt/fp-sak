package no.nav.foreldrepenger.dokumentbestiller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

class VarselRevurderingHåndterer {

    private final Period defaultVenteFrist;
    private final DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private final OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private final OppgaveTjeneste oppgaveTjeneste;
    private final BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    VarselRevurderingHåndterer(Period defaultVenteFrist, OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                               OppgaveTjeneste oppgaveTjeneste,
                               BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                               DokumentBestillerTjeneste dokumentBestillerTjeneste) {
        this.defaultVenteFrist = defaultVenteFrist;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
    }

    void oppdater(Behandling behandling, VarselRevurderingAksjonspunktDto adapter) {
        var bestillBrevDto = new BestillBrevDto(behandling.getId(), behandling.getUuid(), DokumentMalType.VARSEL_OM_REVURDERING, adapter.getFritekst());
        bestillBrevDto.setArsakskode(RevurderingVarslingÅrsak.ANNET);
        dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER);
        settBehandlingPaVent(behandling, adapter.getFrist(), fraDto(adapter.getVenteÅrsakKode()));
    }

    private void settBehandlingPaVent(Behandling behandling, LocalDate frist, Venteårsak venteårsak) {
        opprettTaskAvsluttOppgave(behandling);
        behandlingskontrollTjeneste.settBehandlingPåVentUtenSteg(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT,
            bestemFristForBehandlingVent(frist), venteårsak);
    }

    private void opprettTaskAvsluttOppgave(Behandling behandling) {
        var oppgaveÅrsak = behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK;
        var oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        if (OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaver).isPresent()) {
            oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, oppgaveÅrsak);
        } else if (OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.REGISTRER_SØKNAD, oppgaver).isPresent()) {
            oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, OppgaveÅrsak.REGISTRER_SØKNAD);
        }
    }

    private LocalDateTime bestemFristForBehandlingVent(LocalDate frist) {
        return frist != null
            ? LocalDateTime.of(frist, LocalDateTime.now().toLocalTime())
            : LocalDateTime.now().plus(defaultVenteFrist);
    }

    private Venteårsak fraDto(String kode) {
        return Venteårsak.fraKode(kode);
    }
}
