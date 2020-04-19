package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

// Dokumentmottaker for ytelsesrelaterte dokumenter har felles protokoll som fanges her
// Variasjoner av protokollen håndteres utenfro
public abstract class DokumentmottakerYtelsesesrelatertDokument implements Dokumentmottaker {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DokumentmottakerYtelsesesrelatertDokument.class);

    protected DokumentmottakerFelles dokumentmottakerFelles;
    Behandlingsoppretter behandlingsoppretter;
    Kompletthetskontroller kompletthetskontroller;
    BehandlingRevurderingRepository revurderingRepository;
    protected BehandlingRepository behandlingRepository;
    private ForeldrepengerUttakTjeneste fpUttakTjeneste;

    protected DokumentmottakerYtelsesesrelatertDokument() {
        // For CDI proxy
    }

    @Inject
    public DokumentmottakerYtelsesesrelatertDokument(DokumentmottakerFelles dokumentmottakerFelles,
                                                     Behandlingsoppretter behandlingsoppretter,
                                                     Kompletthetskontroller kompletthetskontroller,
                                                     ForeldrepengerUttakTjeneste fpUttakTjeneste,
                                                     BehandlingRepositoryProvider repositoryProvider) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fpUttakTjeneste = fpUttakTjeneste;
    }

    /* TEMPLATE-metoder som må håndteres spesifikt for hver type av ytelsesdokumenter - START */
    public abstract  void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void håndterKøetBehandling(MottattDokument mottattDokument, Behandling køetBehandling, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType);

    public abstract boolean skalOppretteKøetBehandling(Fagsak fagsak);

    protected abstract void opprettKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling sisteAvsluttetBehandling);
    /* TEMPLATE-metoder SLUTT */

    @Override
    public final void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());

        if (sisteYtelsesbehandling.isEmpty()) {
            håndterIngenTidligereBehandling(fagsak, mottattDokument, behandlingÅrsakType);
            return;
        }

        Behandling behandling = sisteYtelsesbehandling.get();
        boolean sisteYtelseErFerdigbehandlet = sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.FALSE);
        log.info("DYD mottatt dokument {} for fagsak {} sistebehandling {} ferdig {}", mottattDokument.getId(), fagsak.getId(),
            sisteYtelsesbehandling.map(Behandling::getId).orElse(0L), sisteYtelsesbehandling.map(Behandling::getStatus).orElse(BehandlingStatus.OPPRETTET).getKode());
        if (sisteYtelseErFerdigbehandlet) {
            Optional<Behandling> sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
            behandling = sisteAvsluttetBehandling.orElse(behandling);
            // Håndter avsluttet behandling
            if (behandlingsoppretter.erAvslåttBehandling(behandling)
                || behandlingsoppretter.harBehandlingsresultatOpphørt(behandling)) {
                håndterAvslåttEllerOpphørtBehandling(mottattDokument, fagsak, behandling, behandlingÅrsakType);
            } else {
                håndterAvsluttetTidligereBehandling(mottattDokument, fagsak, behandlingÅrsakType);
            }
        } else {
            oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument, behandlingÅrsakType);
        }
    }

    @Override
    public void mottaDokumentForKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> eksisterendeKøetBehandling = revurderingRepository.finnKøetYtelsesbehandling(fagsak.getId());
        if (eksisterendeKøetBehandling.isPresent()) {
            Behandling køetBehandling = eksisterendeKøetBehandling.get();
            dokumentmottakerFelles.opprettHistorikk(køetBehandling, mottattDokument.getJournalpostId());
            dokumentmottakerFelles.opprettKøetHistorikk(køetBehandling, eksisterendeKøetBehandling.isPresent());
            håndterKøetBehandling(mottattDokument, køetBehandling, behandlingÅrsakType);
        } else if (!skalOppretteKøetBehandling(fagsak)) {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); // Skal ikke være mulig for #Sx og #Ix som alltid oppretter køet, men #E12 vil treffe denne
        } else {
            Behandling sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
            opprettKøetBehandling(mottattDokument, fagsak, behandlingÅrsakType, sisteAvsluttetBehandling);
        }
    }

    protected final boolean erAvslag(Behandling avsluttetBehandling) {
        return behandlingsoppretter.erAvslåttBehandling(avsluttetBehandling);
    }

    boolean harAvslåttPeriode(Behandling avsluttetBehandling) {
        final var uttakResultat = fpUttakTjeneste.hentUttakHvisEksisterer(avsluttetBehandling.getId());
        return uttakResultat.map(uttakResultatEntitet -> uttakResultatEntitet.getGjeldendePerioder().stream()
            .anyMatch(periode -> PeriodeResultatType.AVSLÅTT.equals(periode.getResultatType()))).orElse(false);
    }
}
