package no.nav.foreldrepenger.dokumentbestiller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@ApplicationScoped
public class DokumentBehandlingTjeneste {
    private static final Period MANUELT_VENT_FRIST = Period.ofDays(28);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;

    public DokumentBehandlingTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
            OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            OppgaveTjeneste oppgaveTjeneste,
            BehandlingDokumentRepository behandlingDokumentRepository) {
        Objects.requireNonNull(repositoryProvider, "repositoryProvider");
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
    }

    public void loggDokumentBestilt(Behandling behandling, DokumentMalType dokumentMalTypeKode, UUID bestillingUuid) {
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId())
                .orElseGet(() -> BehandlingDokumentEntitet.Builder.ny().medBehandling(behandling.getId()).build());
        behandlingDokument.leggTilBestiltDokument(new BehandlingDokumentBestiltEntitet.Builder()
                .medBehandlingDokument(behandlingDokument)
                .medDokumentMalType(dokumentMalTypeKode.getKode())
                .medBestillingUuid(bestillingUuid)
                .build());
        behandlingDokumentRepository.lagreOgFlush(behandlingDokument);
    }

    public boolean erDokumentBestilt(Long behandlingId, DokumentMalType dokumentMalTypeKode) {
        return behandlingDokumentRepository.hentHvisEksisterer(behandlingId)
            .map(BehandlingDokumentEntitet::getBestilteDokumenter).orElse(List.of()).stream()
                .anyMatch(dok -> dok.getDokumentMalType().equals(dokumentMalTypeKode.getKode()));
    }

    public void nullstillVedtakFritekstHvisFinnes(Long behandlingId) {
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        behandlingDokument.ifPresent(behandlingDokumentEntitet -> behandlingDokumentRepository.lagreOgFlush(
            BehandlingDokumentEntitet.Builder.fraEksisterende(behandlingDokumentEntitet)
                .medVedtakFritekst(null)
                .build()));
    }

    public void settBehandlingPåVent(Long behandlingId, Venteårsak venteårsak) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        opprettTaskAvsluttOppgave(behandling);
        behandlingskontrollTjeneste.settBehandlingPåVentUtenSteg(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT,
                LocalDateTime.now().plus(MANUELT_VENT_FRIST), venteårsak);
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

    public void utvidBehandlingsfristManuelt(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, finnNyFristManuelt(behandling));
    }

    void oppdaterBehandlingMedNyFrist(Behandling behandling, LocalDate nyFrist) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(nyFrist);
        behandlingRepository.lagre(behandling, lås);
    }

    LocalDate finnNyFristManuelt(Behandling behandling) {
        return LocalDate.now().plusWeeks(behandling.getType().getBehandlingstidFristUker());
    }

    public void utvidBehandlingsfristManueltMedlemskap(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, utledFristMedlemskap(behandling, finnAksjonspunktperiodeForVentPåFødsel()));

    }

    public void kvitterBrevSent(DokumentProdusertDto kvittering) {
        var behandling = behandlingRepository.hentBehandling(kvittering.behandlingUuid());
        lagreHistorikkInnslag(kvittering, behandling);
        oppdaterDokumentBestillingMedJournalpostId(kvittering.dokumentbestillingUuid(), kvittering.journalpostId());
    }

    private void lagreHistorikkInnslag(DokumentProdusertDto kvittering, Behandling behandling) {
        var historikkInnslag = HistorikkFraBrevKvitteringMapper.opprettHistorikkInnslag(kvittering, behandling.getId(), behandling.getFagsakId());
        if (historikkRepository.finnesUuidAllerede(historikkInnslag.getUuid())) {
            LOG.info("Oppdaget duplikat historikkinnslag: {}, lagrer ikke.", historikkInnslag.getUuid());
            return;
        }
        historikkRepository.lagre(historikkInnslag);
    }

    private void oppdaterDokumentBestillingMedJournalpostId(UUID bestillingUuid, String journalpostId) {
        var dokumentBestiling = behandlingDokumentRepository.hentHvisEksisterer(bestillingUuid);
        dokumentBestiling.ifPresent(bestilling -> {
            bestilling.setJournalpostId(new JournalpostId(journalpostId));
            behandlingDokumentRepository.lagreOgFlush(bestilling);
        });
    }

    private Period finnAksjonspunktperiodeForVentPåFødsel() {
        var ventPåFødsel = AksjonspunktDefinisjon.VENT_PÅ_FØDSEL;
        return Period.parse(ventPåFødsel.getFristPeriode());
    }

    LocalDate utledFristMedlemskap(Behandling behandling, Period aksjonspunktPeriode) {
        var vanligFrist = finnNyFristManuelt(behandling);
        var terminFrist = beregnTerminFrist(behandling, aksjonspunktPeriode);
        if (terminFrist.isPresent() && vanligFrist.isAfter(terminFrist.get()) && iFremtiden(terminFrist.get())) {
            return terminFrist.get();
        }
        return vanligFrist;
    }

    private boolean iFremtiden(LocalDate dato) {
        return dato.isAfter(LocalDate.now());
    }

    private Optional<LocalDate> beregnTerminFrist(Behandling behandling, Period aksjonspunktPeriode) {
        var gjeldendeTerminBekreftelse = familieHendelseRepository.hentAggregat(behandling.getId())
                .getGjeldendeTerminbekreftelse();
        if (gjeldendeTerminBekreftelse.isPresent()) {
            var oppgittTermindato = gjeldendeTerminBekreftelse.get().getTermindato();
            return Optional.of(oppgittTermindato.plus(aksjonspunktPeriode));
        }
        return Optional.empty();
    }
}
