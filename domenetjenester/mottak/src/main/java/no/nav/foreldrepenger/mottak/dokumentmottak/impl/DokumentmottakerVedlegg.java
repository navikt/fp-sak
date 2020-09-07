package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("VEDLEGG")
class DokumentmottakerVedlegg implements Dokumentmottaker {

    private BehandlingRepository behandlingRepository;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private BehandlingRevurderingRepository revurderingRepository;
    private Kompletthetskontroller kompletthetskontroller;

    @Inject
    public DokumentmottakerVedlegg(BehandlingRepositoryProvider repositoryProvider,
                                   DokumentmottakerFelles dokumentmottakerFelles,
                                   Kompletthetskontroller kompletthetskontroller) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.kompletthetskontroller = kompletthetskontroller;
    }

    @Override
    public void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak, mottattDokument);

        Optional<Behandling> åpenBehandling = revurderingRepository.finnÅpenYtelsesbehandling(fagsak.getId());
        Optional<Behandling> åpenAnnenBehandling = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .filter(b -> !b.erYtelseBehandling()).findFirst();

        if (åpenAnnenBehandling.isPresent()) { // Klage, anke, etc
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, åpenAnnenBehandling.get(), mottattDokument);
        } else if (åpenBehandling.isPresent()) {
            håndterÅpenBehandling(fagsak, åpenBehandling.get(), mottattDokument);
        } else if (skalOppretteNyBehandlingSomFølgerAvVedlegget(mottattDokument, fagsak)) { //#V3
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); //#V1 og #V4
        }
    }

    @Override
    public void mottaDokumentForKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak, mottattDokument);

        Optional<Behandling> eksisterendeKøetBehandling = revurderingRepository.finnKøetYtelsesbehandling(fagsak.getId());
        Optional<Behandling> åpenAnnenBehandling = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .filter(b -> !b.erYtelseBehandling()).findFirst();
        if (åpenAnnenBehandling.isPresent()) { // Klage, anke, etc
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, åpenAnnenBehandling.get(), mottattDokument);
        } else if (eksisterendeKøetBehandling.isPresent()) { //#V5
            kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(eksisterendeKøetBehandling.get(), mottattDokument, Optional.empty());
        } else if (skalOppretteNyBehandlingSomFølgerAvVedlegget(mottattDokument, fagsak)) { //#V7
            dokumentmottakerFelles.opprettKøetFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); //#V1 og #V4
        }
    }

    private boolean skalOppretteNyBehandlingSomFølgerAvVedlegget(MottattDokument mottattDokument, Fagsak fagsak) {
        return dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(fagsak) && !mottattDokumentHarTypeAnnetEllerUdefinert(mottattDokument);
    }

    private boolean mottattDokumentHarTypeAnnetEllerUdefinert(MottattDokument mottattDokument) {
        return DokumentTypeId.ANNET.equals(mottattDokument.getDokumentType()) ||
            DokumentTypeId.UDEFINERT.equals(mottattDokument.getDokumentType()) ||
            !DokumentTypeId.erKodeKjent(mottattDokument.getDokumentType().getKode());
    }

    private void håndterÅpenBehandling(Fagsak fagsak, Behandling behandling, MottattDokument mottattDokument) { //#V2
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, behandling, mottattDokument);
        } else {
            kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        }
    }
}
