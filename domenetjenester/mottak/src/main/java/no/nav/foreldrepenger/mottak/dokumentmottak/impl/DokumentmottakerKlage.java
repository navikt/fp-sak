package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static no.nav.vedtak.feil.LogLevel.WARN;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.klage.KlageFormkravTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("KLAGE")
class DokumentmottakerKlage implements Dokumentmottaker {

    private static final Logger logger = LoggerFactory.getLogger(DokumentmottakerKlage.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private KlageFormkravTjeneste klageFormkravTjeneste;

    @Inject
    public DokumentmottakerKlage(BehandlingRepositoryProvider repositoryProvider, BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                                 DokumentmottakerFelles dokumentmottakerFelles,
                                 KlageFormkravTjeneste klageFormkravTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.klageFormkravTjeneste = klageFormkravTjeneste;
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
            dokumentmottakerFelles.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
            klageFormkravTjeneste.opprettKlage(behandling);
            dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
            dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument);
        });
    }

    private Optional<Behandling> opprettKlagebehandling(Fagsak fagsak) {
        if (behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).isEmpty()) { //#K2
            Feilene.FACTORY.finnerIkkeEksisterendeBehandling(fagsak.getSaksnummer().toString()).log(logger);
            return Optional.empty();
        }
        return Optional.of(behandlingOpprettingTjeneste.opprettBehandlingUtenHistorikk(fagsak, BehandlingType.KLAGE, BehandlingÅrsakType.UDEFINERT));
    }

    private boolean finnesKlageBehandlingForSak(Fagsak fagsak) {
        return behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.KLAGE).isPresent();
    }

    interface Feilene extends DeklarerteFeil {
        Feilene FACTORY = FeilFactory.create(Feilene.class);

        @TekniskFeil(feilkode = "FP-683421", feilmelding = "Fant ingen passende behandling for saksnummer '%s'", logLevel = WARN)
        Feil finnerIkkeEksisterendeBehandling(String saksnummer);
    }
}
