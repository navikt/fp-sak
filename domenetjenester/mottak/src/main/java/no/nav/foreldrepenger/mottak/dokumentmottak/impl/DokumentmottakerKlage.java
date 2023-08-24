package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(DokumentGruppe.KLAGE)
class DokumentmottakerKlage implements Dokumentmottaker {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentmottakerKlage.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private KlageVurderingTjeneste klageVurderingTjeneste;

    @Inject
    public DokumentmottakerKlage(BehandlingRepositoryProvider repositoryProvider, BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                                 DokumentmottakerFelles dokumentmottakerFelles,
                                 KlageVurderingTjeneste klageVurderingTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
    }

    @Override
    public void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        startBehandlingAvKlage(mottattDokument, fagsak);
    }

    @Override
    public void mottaDokumentForKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak,
                                               BehandlingÅrsakType behandlingÅrsakType) {
        startBehandlingAvKlage(mottattDokument, fagsak);
    }

    void startBehandlingAvKlage(MottattDokument mottattDokument, Fagsak fagsak) {
        if (finnesKlageBehandlingForSak(fagsak) || DokumentTypeId.KLAGE_ETTERSENDELSE.equals(mottattDokument.getDokumentType())) {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); //#K3
            return;
        }
        opprettKlagebehandling(fagsak).ifPresent(behandling -> { //#K1
            dokumentmottakerFelles.persisterDokumentinnhold(behandling, mottattDokument);
            klageVurderingTjeneste.hentEvtOpprettKlageResultat(behandling);
            dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
            dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument);
        });
    }

    private Optional<Behandling> opprettKlagebehandling(Fagsak fagsak) {
        if (behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).isEmpty()) { //#K2
            LOG.warn("FP-683421 Fant ingen passende behandling for saksnummer {}", fagsak.getSaksnummer());
            return Optional.empty();
        }
        return Optional.of(behandlingOpprettingTjeneste.opprettBehandlingUtenHistorikk(fagsak, BehandlingType.KLAGE, BehandlingÅrsakType.UDEFINERT));
    }

    private boolean finnesKlageBehandlingForSak(Fagsak fagsak) {
        return behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.KLAGE).isPresent();
    }

}
