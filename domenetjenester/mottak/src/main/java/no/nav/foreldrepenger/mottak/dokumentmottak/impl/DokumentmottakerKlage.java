package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static no.nav.vedtak.feil.LogLevel.WARN;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.klage.KlageFormkravTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("KLAGE")
class DokumentmottakerKlage implements Dokumentmottaker {

    private static final Logger logger = LoggerFactory.getLogger(DokumentmottakerKlage.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private KlageFormkravTjeneste klageFormkravTjeneste;

    @Inject
    public DokumentmottakerKlage(BehandlingRepositoryProvider repositoryProvider, BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                 DokumentmottakerFelles dokumentmottakerFelles, MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                 KlageFormkravTjeneste klageFormkravTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.klageFormkravTjeneste = klageFormkravTjeneste;
    }

    @Override
    public void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, DokumentTypeId dokumentTypeId, BehandlingÅrsakType behandlingÅrsakType) {
        startBehandlingAvKlage(mottattDokument, dokumentTypeId, fagsak);
    }

    @Override
    public void mottaDokumentForKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, DokumentTypeId dokumentTypeId,
                                               BehandlingÅrsakType behandlingÅrsakType) {
        startBehandlingAvKlage(mottattDokument, dokumentTypeId, fagsak);
    }

    void startBehandlingAvKlage(MottattDokument mottattDokument, DokumentTypeId dokumentTypeId, Fagsak fagsak) {
        if (finnesKlageBehandlingForSak(fagsak) || DokumentTypeId.KLAGE_ETTERSENDELSE.equals(dokumentTypeId)) {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); //#K3
            return;
        }
        opprettKlagebehandling(fagsak).ifPresent(behandling -> { //#K1
            mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
            klageFormkravTjeneste.opprettKlage(behandling);
            dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
            dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument.getJournalpostId());
        });
    }

    private Optional<Behandling> opprettKlagebehandling(Fagsak fagsak) {
        Optional<Behandling> forrigeOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (!forrigeOpt.isPresent()) { //#K2
            Feilene.FACTORY.finnerIkkeEksisterendeBehandling(fagsak.getSaksnummer().toString()).log(logger);
            return Optional.empty();
        }
        Behandling behandlingKlagenGjelder = forrigeOpt.get();
        BehandlingType behandlingTypeKlage = BehandlingType.KLAGE;
        return Optional.ofNullable(behandlingskontrollTjeneste.opprettNyBehandling(fagsak, behandlingTypeKlage,
            (beh) -> {
                beh.setBehandlingstidFrist(FPDateUtil.iDag().plusWeeks(behandlingTypeKlage.getBehandlingstidFristUker()));
                beh.setBehandlendeEnhet(dokumentmottakerFelles.utledEnhetFraTidligereBehandling(behandlingKlagenGjelder));
            }));
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
