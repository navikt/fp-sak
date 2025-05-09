package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(DokumentGruppe.VEDLEGG)
class DokumentmottakerVedlegg implements Dokumentmottaker {

    private final BehandlingRepository behandlingRepository;
    private final DokumentmottakerFelles dokumentmottakerFelles;
    private final BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    private final Kompletthetskontroller kompletthetskontroller;

    @Inject
    public DokumentmottakerVedlegg(BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                                   DokumentmottakerFelles dokumentmottakerFelles,
                                   Kompletthetskontroller kompletthetskontroller,
                                   BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.kompletthetskontroller = kompletthetskontroller;
    }

    @Override
    public void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {

        var åpenBehandling = behandlingRevurderingTjeneste.finnÅpenYtelsesbehandling(fagsak.getId());
        var andreÅpneBehandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .filter(b -> !b.erYtelseBehandling()).toList();
        var åpenKlageSomKanVurdereDokument = andreÅpneBehandlinger.stream()
            .filter(b -> BehandlingType.KLAGE.equals(b.getType()))
            .filter(b -> !BehandlendeEnhetTjeneste.getKlageInstans().equals(b.getBehandlendeOrganisasjonsEnhet()))
            .findFirst();
        var historikkBehandling = åpenBehandling.or(() -> åpenKlageSomKanVurdereDokument).orElse(null);
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak, historikkBehandling, mottattDokument);

        if (åpenBehandling.isPresent()) {
            kompletthetskontroller.persisterDokumentOgVurderKompletthet(åpenBehandling.get(), mottattDokument);
        } else if (skalOppretteNyFørstegangsBehandlingSomFølgerAvVedlegget(mottattDokument, fagsak)) { //#V3
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else if (åpenKlageSomKanVurdereDokument.isPresent()) {
            kompletthetskontroller.vedleggHåndteresGjennomÅpenKlage(åpenKlageSomKanVurdereDokument.get(), mottattDokument);
        } else {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
        }
    }

    @Override
    public void mottaDokumentForKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {

        var eksisterendeKøetBehandling = behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsak.getId());
        var andreÅpneBehandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .filter(b -> !b.erYtelseBehandling()).toList();
        var åpenKlageSomKanVurdereDokument = andreÅpneBehandlinger.stream()
            .filter(b -> BehandlingType.KLAGE.equals(b.getType()))
            .filter(b -> !BehandlendeEnhetTjeneste.getKlageInstans().equals(b.getBehandlendeOrganisasjonsEnhet()))
            .findFirst();
        var historikkBehandling = eksisterendeKøetBehandling.or(() -> åpenKlageSomKanVurdereDokument).orElse(null);
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak, historikkBehandling, mottattDokument);

        if (eksisterendeKøetBehandling.isPresent()) {
            kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(eksisterendeKøetBehandling.get(), mottattDokument, Optional.empty());
        } else if (skalOppretteNyFørstegangsBehandlingSomFølgerAvVedlegget(mottattDokument, fagsak)) { //#V7
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else if (åpenKlageSomKanVurdereDokument.isPresent()) {
            kompletthetskontroller.vedleggHåndteresGjennomÅpenKlage(åpenKlageSomKanVurdereDokument.get(), mottattDokument);
        } else {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
        }
    }

    private boolean skalOppretteNyFørstegangsBehandlingSomFølgerAvVedlegget(MottattDokument mottattDokument, Fagsak fagsak) {
        return dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(fagsak) && !mottattDokumentHarTypeAnnetEllerUdefinert(mottattDokument);
    }

    private boolean mottattDokumentHarTypeAnnetEllerUdefinert(MottattDokument mottattDokument) {
        var fraDokument = mottattDokument.getDokumentType();
        return fraDokument.erAnnenDokType() || DokumentTypeId.UDEFINERT.equals(fraDokument);
    }
}
