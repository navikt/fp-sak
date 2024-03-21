package no.nav.foreldrepenger.dokumentbestiller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@ApplicationScoped
public class DokumentBehandlingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentBehandlingTjeneste.class);

    private static final Period MANUELT_VENT_FRIST = Period.ofDays(28);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private HistorikkRepository historikkRepository;

    public DokumentBehandlingTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                      BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                      BehandlingDokumentRepository behandlingDokumentRepository) {
        Objects.requireNonNull(repositoryProvider, "repositoryProvider");
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
    }

    public void loggDokumentBestilt(Behandling behandling, DokumentBestilling bestilling) {
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId())
            .orElseGet(() -> BehandlingDokumentEntitet.Builder.ny().medBehandling(behandling.getId()).build());

        behandlingDokument.leggTilBestiltDokument(new BehandlingDokumentBestiltEntitet.Builder()
            .medBehandlingDokument(behandlingDokument)
            .medDokumentMalType(bestilling.dokumentMal().getKode())
            .medBestillingUuid(bestilling.bestillingUuid())
            .medOpprinneligDokumentMal(Optional.ofNullable(bestilling.journalførSom()).map(DokumentMalType::getKode).orElse(null))
            .build());

        behandlingDokumentRepository.lagreOgFlush(behandlingDokument);
    }

    public boolean erDokumentBestilt(Long behandlingId, DokumentMalType dokumentMalTypeKode) {
        return behandlingDokumentRepository.hentHvisEksisterer(behandlingId)
            .map(BehandlingDokumentEntitet::getBestilteDokumenter)
            .orElse(List.of())
            .stream()
            .anyMatch(dok -> dok.getDokumentMalType().equals(dokumentMalTypeKode.getKode()) || dokumentMalTypeKode.getKode()
                .equals(dok.getOpprineligDokumentMal()));
    }

    public boolean erDokumentBestiltForFagsak(Long fagsakId, DokumentMalType dokumentMalTypeKode) {
        return behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsakId).stream()
            .anyMatch(b -> erDokumentBestilt(b.getId(), dokumentMalTypeKode));
    }

    public void nullstillVedtakFritekstHvisFinnes(Long behandlingId) {
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        behandlingDokument.ifPresent(behandlingDokumentEntitet -> behandlingDokumentRepository.lagreOgFlush(
            BehandlingDokumentEntitet.Builder.fraEksisterende(behandlingDokumentEntitet).medVedtakFritekst(null).build()));
    }

    public void settBehandlingPåVent(Long behandlingId, Venteårsak venteårsak) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        behandlingskontrollTjeneste.settBehandlingPåVentUtenSteg(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT,
            LocalDateTime.now().plus(MANUELT_VENT_FRIST), venteårsak);
    }

    public void utvidBehandlingsfristManuelt(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, finnNyFristManuelt(behandling.getType()));
    }

    public void utvidBehandlingsfristManueltMedlemskap(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, utledFristMedlemskap(behandling));

    }

    void oppdaterBehandlingMedNyFrist(Behandling behandling, LocalDate nyFrist) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(nyFrist);
        behandlingRepository.lagre(behandling, lås);
    }

    public void kvitterSendtBrev(DokumentKvittering kvittering) {
        var behandling = behandlingRepository.hentBehandling(kvittering.behandlingUuid());
        var bestillingUuid = kvittering.bestillingUuid();
        var dokumentBestiling = behandlingDokumentRepository.hentHvisEksisterer(bestillingUuid);
        if (dokumentBestiling.isPresent()) {
            var bestilling = dokumentBestiling.get();
            var journalpostId = kvittering.journalpostId();
            if (Objects.isNull(bestilling.getJournalpostId())) { // behandlinger med verge produserer to brev per bestilling - vi ble enig om å ignorere det andre.
                bestilling.setJournalpostId(new JournalpostId(journalpostId));
                LOG.trace("JournalpostId: {}.", journalpostId);
                behandlingDokumentRepository.lagreOgFlush(bestilling);
            }
            var dokumentMal = utledMalBrukt(bestilling.getDokumentMalType(), bestilling.getOpprineligDokumentMal());
            lagreHistorikk(behandling, dokumentMal, journalpostId, kvittering.dokumentId());
        } else {
            LOG.warn("Fant ikke dokument bestilling for bestillingUuid: {}.", bestillingUuid);
        }
    }

    private void lagreHistorikk(Behandling behandling, DokumentMalType dokumentMalBrukt, String journalpostId, String dokumentId) {
        var historikkInnslag = HistorikkFraDokumentKvitteringMapper
            .opprettHistorikkInnslag(dokumentMalBrukt, journalpostId, dokumentId, behandling.getId(), behandling.getFagsakId());
        historikkRepository.lagre(historikkInnslag);
    }

    private DokumentMalType utledMalBrukt(String dokumentMalType, String opprineligDokumentMal) {
        var dokumentMal = DokumentMalType.fraKode(dokumentMalType);
        if (DokumentMalType.FRITEKSTBREV.equals(dokumentMal) && opprineligDokumentMal != null) {
            return DokumentMalType.fraKode(opprineligDokumentMal);
        }
        return dokumentMal;
    }

    LocalDate utledFristMedlemskap(Behandling behandling) {
        var vanligFrist = finnNyFristManuelt(behandling.getType());
        return beregnTerminFrist(behandling).filter(f -> f.isAfter(vanligFrist) && f.isAfter(LocalDate.now())).orElse(vanligFrist);
    }

    LocalDate finnNyFristManuelt(BehandlingType behandlingType) {
        return LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker());
    }

    private Optional<LocalDate> beregnTerminFrist(Behandling behandling) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
            .map(t -> t.plusWeeks(behandling.getType().getBehandlingstidFristUker()));
    }
}
