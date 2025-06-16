package no.nav.foreldrepenger.behandlingsprosess.prosessering;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;

/**
 * Grensesnitt for å kjøre behandlingsprosess, herunder gjenopptak,
 * registeroppdatering, koordinering av sakskompleks mv. Alle kall til
 * utføringsmetode i behandlingskontroll bør gå gjennom tasks opprettet her.
 * Merk Dem: - ta av vent og grunnlagsoppdatering kan føre til reposisjonering
 * av behandling til annet steg - grunnlag endres ved ankomst av dokument, ved
 * registerinnhenting og ved senere overstyring ("bekreft AP" eller egne
 * overstyringAP) - Hendelser: Ny behandling (Manuell, dokument, mv), Gjenopptak
 * (Manuell/Frist), Interaktiv (Oppdater/Fortsett), Dokument, Datahendelse,
 * Vedtak, KØ-hendelser
 **/
public interface BehandlingProsesseringTjeneste {

    // Støttefunksjon for å vurdere behov for interaktiv oppdatering, ref invalid-at-midnight
    boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling);

    // Støttefunksjon for å sørge for at registerdata vil bli oppdatert, gjør ikke oppdatering
    void tvingInnhentingRegisteropplysninger(Behandling behandling);

    // Har behandlingen oppgitt steg i modellen?
    boolean erStegAktueltForBehandling(Behandling behandling, BehandlingStegType stegType);

    boolean erBehandlingFørSteg(Behandling behandling, BehandlingStegType stegType);

    boolean erBehandlingEtterSteg(Behandling behandling, BehandlingStegType stegType);

    // AV/PÅ Vent
    void taBehandlingAvVent(Behandling behandling);

    // Steg ikke vesentlig
    void settBehandlingPåVentUtenSteg(Behandling behandling, AksjonspunktDefinisjon apDef, LocalDateTime fristTid, Venteårsak venteårsak);
    // Steg kan ha betydning for gjenopptak
    void settBehandlingPåVent(Behandling behandling, BehandlingStegType steg, AksjonspunktDefinisjon apDef, LocalDateTime fristTid, Venteårsak venteårsak);

    // For snapshot av grunnlag før man gjør andre endringer enn registerinnhenting
    EndringsresultatSnapshot taSnapshotAvBehandlingsgrunnlag(Behandling behandling);

    // Spole prosessen basert på diff. Til bruk ved grunnlagsendringer utenom register (søknad)
    void utledDiffOgReposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatSnapshot snapshot);

    // Spole til spesifikt steg
    void reposisjonerBehandlingTilbakeTil(Behandling behandling, BehandlingLås lås, BehandlingStegType stegType);

    /**
     * Returnerer tasks for oppdatering/fortsett for bruk med
     * BehandlingskontrollAsynkTjeneste. Blir ikke lagret her
     */
    Optional<String> finnesTasksForPolling(Behandling behandling);
    ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling);

    // Til bruk ved første prosessering av nyopprettet behandling. Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForStartBehandling(Behandling behandling);

    // Til bruk for å kjøre behandlingsprosessen videre. Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForFortsettBehandling(Behandling behandling);

    // Til bruk for å kjøre behandlingsprosessen videre. Lagrer tasks. Setter autopunkt til utført. Returnerer gruppe-handle
    String opprettTasksForFortsettBehandlingSettUtført(Behandling behandling, Optional<AksjonspunktDefinisjon> autoPunktUtført);

    // Brukes kun der et steg har suspendet seg selv. Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForFortsettBehandlingResumeStegNesteKjøring(Behandling behandling, BehandlingStegType stegType,
                                                                   LocalDateTime nesteKjøringEtter);

    // Robust task til bruk ved gjenopptak fra vent (eller annen tilstand)
    // (Hendelse: Manuell input, Frist utløpt, mv)
    // NB oppdaterer registerdata Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling, LocalDateTime nesteKjøringEtter);
    String opprettTasksForGjenopptaOppdaterFortsettBatch(Behandling behandling, LocalDateTime nesteKjøringEtter);

    String opprettTasksForInitiellRegisterInnhenting(Behandling behandling);

    Set<Long> behandlingerMedFeiletProsessTask();
}
