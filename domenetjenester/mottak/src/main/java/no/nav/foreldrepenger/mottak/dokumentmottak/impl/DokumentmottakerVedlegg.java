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
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("VEDLEGG")
class DokumentmottakerVedlegg implements Dokumentmottaker {

    private BehandlingRepository behandlingRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private BehandlingRevurderingRepository revurderingRepository;
    private Kompletthetskontroller kompletthetskontroller;

    @Inject
    public DokumentmottakerVedlegg(BehandlingRepositoryProvider repositoryProvider,
                                   DokumentmottakerFelles dokumentmottakerFelles,
                                   Behandlingsoppretter behandlingsoppretter,
                                   Kompletthetskontroller kompletthetskontroller) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingsoppretter = behandlingsoppretter;
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.kompletthetskontroller = kompletthetskontroller;
    }

    @Override
    public void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, DokumentTypeId dokumentTypeId, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(mottattDokument.getFagsakId(), mottattDokument.getJournalpostId(), dokumentTypeId);

        Optional<Behandling> åpenBehandling = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .findFirst();

        if (åpenBehandling.isPresent()) {
            håndterÅpenBehandling(fagsak, åpenBehandling.get(), mottattDokument);
        } else {
            if (skalOppretteNyBehandlingSomFølgerAvVedlegget(mottattDokument, fagsak)) { //#V3
                Optional<Behandling> behandlingOptional = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
                dokumentmottakerFelles.opprettNyFørstegangFraAvslag(mottattDokument, fagsak, behandlingOptional.get()); // NOSONAR
            } else {
                dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); //#V1 og #V4
            }
        }
    }

    @Override
    public void mottaDokumentForKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, DokumentTypeId dokumentTypeId, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(mottattDokument.getFagsakId(), mottattDokument.getJournalpostId(), dokumentTypeId);

        Optional<Behandling> eksisterendeKøetBehandling = revurderingRepository.finnKøetYtelsesbehandling(fagsak.getId());
        Behandling køetBehandling = eksisterendeKøetBehandling
            .orElseGet(() -> skalOppretteNyBehandlingSomFølgerAvVedlegget(mottattDokument, fagsak)
                ? behandlingsoppretter.opprettKøetBehandling(fagsak, BehandlingÅrsakType.RE_ANNET) : null);
        if (køetBehandling != null) { //#V5 og #V7
            dokumentmottakerFelles.opprettKøetHistorikk(køetBehandling, eksisterendeKøetBehandling.isPresent());
            kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(køetBehandling, mottattDokument, Optional.empty());
        } else { //#V6
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
        }
    }

    private boolean skalOppretteNyBehandlingSomFølgerAvVedlegget(MottattDokument mottattDokument, Fagsak fagsak) {
        return dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(fagsak) && !mottattDokumentHarTypeAnnetEllerUdefinert(mottattDokument);
    }

    private boolean mottattDokumentHarTypeAnnetEllerUdefinert(MottattDokument mottattDokument) {
        return DokumentTypeId.ANNET.equals(mottattDokument.getDokumentType()) || DokumentTypeId.UDEFINERT.equals(mottattDokument.getDokumentType());
    }

    private void håndterÅpenBehandling(Fagsak fagsak, Behandling behandling, MottattDokument mottattDokument) { //#V2
        /** TODO (essv): Digitalen - løfte {@link FagsakYtelseType.ENGANGSTØNAD} til protokoll for Startpunkt,
         * slik at samme protokoll som for FP kan brukes */
        if (fagsak.getYtelseType().equals(FagsakYtelseType.FORELDREPENGER) && behandling.erYtelseBehandling()) {
            kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        } else {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, behandling, mottattDokument);
        }
    }
}
