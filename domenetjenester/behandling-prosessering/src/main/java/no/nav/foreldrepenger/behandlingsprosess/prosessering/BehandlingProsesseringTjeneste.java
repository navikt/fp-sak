package no.nav.foreldrepenger.behandlingsprosess.prosessering;

import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;

/**
 * Grensesnitt for å kjøre behandlingsprosess, herunder gjenopptak, registeroppdatering, koordinering av sakskompleks mv.
 * Alle kall til utføringsmetode i behandlingskontroll bør gå gjennom tasks opprettet her.
 * Merk Dem:
 *   - ta av vent og grunnlagsoppdatering kan føre til reposisjonering av behandling til annet steg
 *   - grunnlag endres ved ankomst av dokument, ved registerinnhenting og ved senere overstyring ("bekreft AP" eller egne overstyringAP)
 *   - Hendelser: Ny behandling (Manuell, dokument, mv), Gjenopptak (Manuell/Frist), Interaktiv (Oppdater/Fortsett), Dokument, Datahendelse, Vedtak, KØ-hendelser
 **/
public interface BehandlingProsesseringTjeneste {

    // Støttefunksjon for å vurdere behov for interaktiv oppdatering, ref invalid-at-midnight
    boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling);

    // Støttefunksjon for å sørge for at registerdata vil bli oppdatert, gjør ikke oppdatering
    void tvingInnhentingRegisteropplysninger(Behandling behandling);

    // Har behandlingen oppgitt steg i modellen?
    boolean erStegAktueltForBehandling(Behandling behandling, BehandlingStegType behandlingStegType);

    // AV/PÅ Vent
    void taBehandlingAvVent(Behandling behandling);
    void settBehandlingPåVent(Behandling behandling, AksjonspunktDefinisjon apDef, LocalDateTime fristTid, Venteårsak venteårsak);

    // For snapshot av grunnlag før man gjør andre endringer enn registerinnhenting
    EndringsresultatSnapshot taSnapshotAvBehandlingsgrunnlag(Behandling behandling);

    // Returnerer snapshot av grunnlag før registerinnhentingen. Forutsetter at behandling ikke er på vent.
    EndringsresultatSnapshot oppdaterRegisterdata(Behandling behandling);

    // Returnerer endringer i grunnlag mellom snapshot og nåtilstand
    EndringsresultatDiff finnGrunnlagsEndring(Behandling behandling, EndringsresultatSnapshot før);

    // Spole prosessen basert på diff. Til bruk ved grunnlagsendringer utenom register (søknad)
    void reposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatDiff grunnlagDiff);

    // Spole til spesifikt steg
    void reposisjonerBehandlingTilbakeTil(Behandling behandling, BehandlingStegType stegType);

    // Registeroppdatering og spoling til rett steg
    void oppdaterRegisterdataReposisjonerVedEndringer(Behandling behandling);

    /** Returnerer tasks for oppdatering/fortsett for bruk med BehandlingskontrollAsynkTjeneste. Blir ikke lagret her */
    ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling);

    // Til bruk ved første prosessering av nyopprettet behandling. Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForStartBehandling(Behandling behandling);

    // Til bruk for å kjøre behandlingsprosessen videre. Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForFortsettBehandling(Behandling behandling);
    String opprettTasksForFortsettBehandlingSettUtført(Behandling behandling, Optional<AksjonspunktDefinisjon> autoPunktUtført);
    String opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(Behandling behandling, BehandlingStegType behandlingStegType, LocalDateTime nesteKjøringEtter);
    // For evt å differensiere på gjenopptak i fortsettbehandling
    String opprettTasksForGjenopptaFortsett(Behandling behandling);

    // Metodene nedenfor oppdaterer registerdata, reposisjonerer behandling og kjører prosessen derfra
    // Til bruk for å oppdatere registerdata og så kjøre behandlingsprosessen videre. Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForOppdaterFortsett(Behandling behandling);

    // Robust task til bruk ved gjenopptak fra vent (eller annen tilstand) (Hendelse: Manuell input, Frist utløpt, mv)
    // NB oppdaterer registerdata Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling);

    String opprettTasksForInitiellRegisterInnhenting(Behandling behandling);
}
