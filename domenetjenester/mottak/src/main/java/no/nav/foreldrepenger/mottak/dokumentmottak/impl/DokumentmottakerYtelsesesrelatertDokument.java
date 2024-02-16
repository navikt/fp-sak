package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

// Dokumentmottaker for ytelsesrelaterte dokumenter har felles protokoll som fanges her
// Variasjoner av protokollen håndteres utenfro
public abstract class DokumentmottakerYtelsesesrelatertDokument implements Dokumentmottaker {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentmottakerYtelsesesrelatertDokument.class);

    protected DokumentmottakerFelles dokumentmottakerFelles;
    Behandlingsoppretter behandlingsoppretter;
    Kompletthetskontroller kompletthetskontroller;
    BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
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
                                                     BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                                                     BehandlingRepository behandlingRepository) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fpUttakTjeneste = fpUttakTjeneste;
    }

    /* TEMPLATE-metoder som må håndteres spesifikt for hver type av ytelsesdokumenter - START */
    public abstract  void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void håndterKøetBehandling(MottattDokument mottattDokument, Behandling køetBehandling, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void håndterUtsattStartdato(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);

    public abstract boolean skalOppretteKøetBehandling(Fagsak fagsak);

    protected abstract void opprettKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling sisteAvsluttetBehandling);
    /* TEMPLATE-metoder SLUTT */

    @Override
    public final void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var sisteYtelsesbehandling = behandlingRevurderingTjeneste.hentAktivIkkeBerørtEllerSisteYtelsesbehandling(fagsak.getId());

        if (sisteYtelsesbehandling.isEmpty()) {
            håndterIngenTidligereBehandling(fagsak, mottattDokument, behandlingÅrsakType);
            return;
        }

        var behandling = sisteYtelsesbehandling.get();
        var sisteYtelseErFerdigbehandlet = behandling.erSaksbehandlingAvsluttet();
        LOG.info("DYD mottatt dokument {} for fagsak {} sistebehandling {} ferdig {}", mottattDokument.getId(), fagsak.getId(),
            behandling.getId(), behandling.getStatus().getKode());
        if (sisteYtelseErFerdigbehandlet) {
            var sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
            behandling = sisteAvsluttetBehandling.orElse(behandling);
            // Håndter avsluttet behandling
            if (behandlingsoppretter.erAvslåttBehandling(behandling)
                || behandlingsoppretter.erOpphørtBehandling(behandling)) {
                håndterAvslåttEllerOpphørtBehandling(mottattDokument, fagsak, behandling, behandlingÅrsakType);
            } else if (behandlingsoppretter.erUtsattBehandling(behandling)) {
                håndterUtsattStartdato(mottattDokument, fagsak, behandlingÅrsakType);
            } else {
                håndterAvsluttetTidligereBehandling(mottattDokument, fagsak, behandlingÅrsakType);
            }
        } else {
            oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument, behandlingÅrsakType);
        }
    }

    @Override
    public void mottaDokumentForKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var eksisterendeKøetBehandling = behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsak.getId());
        var eksisterendeÅpenBehandlingUtenSøknad = behandlingRevurderingTjeneste.finnÅpenYtelsesbehandling(fagsak.getId())
            .filter(b -> b.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD));
        if (eksisterendeÅpenBehandlingUtenSøknad.isPresent()) {
            oppdaterÅpenBehandlingMedDokument(eksisterendeÅpenBehandlingUtenSøknad.get(), mottattDokument, behandlingÅrsakType);
        } else if (eksisterendeKøetBehandling.isPresent()) {
            var køetBehandling = eksisterendeKøetBehandling.get();
            dokumentmottakerFelles.opprettHistorikk(køetBehandling, mottattDokument);
            dokumentmottakerFelles.opprettKøetHistorikk(køetBehandling, true);
            håndterKøetBehandling(mottattDokument, køetBehandling, behandlingÅrsakType);
        } else if (!skalOppretteKøetBehandling(fagsak)) {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); // Skal ikke være mulig for #Sx og #Ix som alltid oppretter køet, men #E12 vil treffe denne
        } else {
            var sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
            opprettKøetBehandling(mottattDokument, fagsak, behandlingÅrsakType, sisteAvsluttetBehandling);
        }
    }

    @Override
    public boolean endringSomUtsetterStartdato(MottattDokument mottattDokument, Fagsak fagsak) {
        return FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType()) &&
            !RelasjonsRolleType.erMor(fagsak.getRelasjonsRolleType()) &&
            mottattDokument.getDokumentType().erForeldrepengeSøknad() &&
            dokumentmottakerFelles.endringSomUtsetterStartdato(mottattDokument, fagsak);
    }

    @Override
    public void mottaUtsettelseAvStartdato(MottattDokument mottattDokument, Fagsak fagsak) {
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType()) &&
            mottattDokument.getDokumentType().erForeldrepengeSøknad()) {
            dokumentmottakerFelles.opprettAnnulleringsBehandlinger(mottattDokument, fagsak);
        } else {
            throw new IllegalArgumentException(String.format("Utviklerfeil: skal ikke kalles for ytelse %s dokumenttype %s",
                fagsak.getYtelseType().getKode(), mottattDokument.getDokumentType().getKode()));
        }
    }

    protected final boolean erAvslag(Behandling avsluttetBehandling) {
        return behandlingsoppretter.erAvslåttBehandling(avsluttetBehandling);
    }

    protected final boolean erOpphør(Behandling avsluttetBehandling) {
        return behandlingsoppretter.erOpphørtBehandling(avsluttetBehandling);
    }

    boolean harInnvilgetPeriode(Behandling avsluttetBehandling) {
        return fpUttakTjeneste.hentUttakHvisEksisterer(avsluttetBehandling.getId())
            .map(uttak -> uttak.getGjeldendePerioder().stream()
                .anyMatch(periode -> PeriodeResultatType.INNVILGET.equals(periode.getResultatType())))
            .orElse(false);
    }
}
