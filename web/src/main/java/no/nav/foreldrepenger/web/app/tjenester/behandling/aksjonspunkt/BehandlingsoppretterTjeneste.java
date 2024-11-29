package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
public class BehandlingsoppretterTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingsoppretterTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SaksbehandlingDokumentmottakTjeneste saksbehandlingDokumentmottakTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    BehandlingsoppretterTjeneste() {
        // CDI
    }

    @Inject
    public BehandlingsoppretterTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                        SaksbehandlingDokumentmottakTjeneste saksbehandlingDokumentmottakTjeneste,
                                        BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        Objects.requireNonNull(behandlingRepositoryProvider, "behandlingRepositoryProvider");
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.mottatteDokumentRepository = behandlingRepositoryProvider.getMottatteDokumentRepository();
        this.saksbehandlingDokumentmottakTjeneste = saksbehandlingDokumentmottakTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    public boolean kanOppretteNyBehandlingAvType(Long fagsakId, BehandlingType type) {
        var finnesÅpneBehandlingerAvType = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsakId)
            .stream()
            .filter(b -> !(type.erKlageAnkeType() && BehandlendeEnhetTjeneste.getKlageInstans().enhetId().equals(b.getBehandlendeEnhet())))
            .map(Behandling::getType)
            .anyMatch(type::equals);
        if (finnesÅpneBehandlingerAvType) {
            return false;
        }
        return switch (type) {
            case KLAGE -> behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsakId).stream()
                .filter(Behandling::erSaksbehandlingAvsluttet)
                .anyMatch(b -> BehandlingType.FØRSTEGANGSSØKNAD.equals(b.getType()));
            case INNSYN -> true;
            case REVURDERING -> kanOppretteRevurdering(fagsakId);
            case FØRSTEGANGSSØKNAD -> kanOppretteFørstegangsbehandling(fagsakId);
            default -> false;
        };
    }

    /**
     * Opprett ny behandling. Returner Prosess Task gruppe for å ta den videre.
     */
    public void opprettNyFørstegangsbehandling(Long fagsakId, Saksnummer saksnummer, boolean erEtterKlageBehandling) {
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        if (erLovÅOppretteNyBehandling(behandlinger)) {
            if (erEtterKlageBehandling && !erEtterKlageGyldigValg(fagsakId)) {
                throw kanIkkeOppretteNyFørstegangsbehandlingEtterKlage(fagsakId);
            }
            var behandlingÅrsakType = erEtterKlageBehandling ? BehandlingÅrsakType.ETTER_KLAGE : BehandlingÅrsakType.UDEFINERT;
            doOpprettNyBehandlingIgjennomMottak(fagsakId, saksnummer, behandlingÅrsakType);
        } else {
            throw kanIkkeOppretteNyFørstegangsbehandling(fagsakId);
        }
    }

    private void doOpprettNyBehandlingIgjennomMottak(Long fagsakId, Saksnummer saksnummer, BehandlingÅrsakType behandlingÅrsakType) {
        var sisteMottatteSøknad = finnSisteMottatteSøknadPåFagsak(fagsakId);
        if (sisteMottatteSøknad != null) {
            opprettNyBehandlingFraSøknad(behandlingÅrsakType, sisteMottatteSøknad, saksnummer);
        } else {
            opprettNyBehandlingFraInntektsmelding(fagsakId, behandlingÅrsakType);
        }
    }

    private void opprettNyBehandlingFraSøknad(BehandlingÅrsakType behandlingÅrsakType,
                                              MottattDokument sisteMottatteSøknad,
                                              Saksnummer saksnummer) {
        if (sisteMottatteSøknad.getBehandlingId() != null) {
            if (sisteMottatteSøknad.getPayloadXml() == null) {
                // For å registrere papirsøknad på nytt ....
                var nyMottatt = new MottattDokument.Builder(sisteMottatteSøknad).build();
                saksbehandlingDokumentmottakTjeneste.dokumentAnkommet(nyMottatt, behandlingÅrsakType, saksnummer);
            } else {
                var sisteBehandling = behandlingRepository.hentBehandling(sisteMottatteSøknad.getBehandlingId());
                saksbehandlingDokumentmottakTjeneste.opprettFraTidligereBehandling(sisteMottatteSøknad, sisteBehandling,
                    behandlingÅrsakType);
            }
        } else {
            saksbehandlingDokumentmottakTjeneste.mottaUbehandletSøknad(sisteMottatteSøknad, behandlingÅrsakType, saksnummer);
        }
    }

    private void opprettNyBehandlingFraInntektsmelding(Long fagsakId, BehandlingÅrsakType behandlingÅrsakType) {
        var sisteMottatteInntektsmelding = finnSisteMottatteInntektsmeldingPåFagsak(fagsakId);
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        if (sisteMottatteInntektsmelding != null && sisteBehandling.isPresent()) {
            saksbehandlingDokumentmottakTjeneste.opprettFraTidligereBehandling(sisteMottatteInntektsmelding,
                sisteBehandling.get(), behandlingÅrsakType);
        } else {
            throw ingenSøknadEllerImÅOppretteNyFørstegangsbehandlingPå(fagsakId);
        }
    }

    public void henleggÅpenFørstegangsbehandlingOgOpprettNy(Long fagsakId, Saksnummer saksnummer) {
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        if (harMinstEnÅpenFørstegangsbehandling(behandlinger)) {
            doOpprettNyBehandlingIgjennomMottak(fagsakId, BehandlingÅrsakType.UDEFINERT);
        } else {
            throw kanIkkeHenleggeÅpenBehandlingOgOppretteNyFørstegangsbehandling(fagsakId);
        }
    }

    public Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, String opprettetAv) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType())
            .orElseThrow();
        var kanRevurderingOpprettes = kanOppretteRevurdering(fagsak.getId());
        if (!kanRevurderingOpprettes) {
            throw kanIkkeOppretteRevurdering(fagsak.getSaksnummer());
        }

        return revurderingTjeneste.opprettManuellRevurdering(fagsak, behandlingÅrsakType, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak), opprettetAv);
    }

    private boolean kanOppretteRevurdering(Long fagsakId) {
        var finnesÅpneBehandlingerAvType = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsakId)
            .stream()
            .anyMatch(b -> BehandlingType.FØRSTEGANGSSØKNAD.equals(b.getType()) || BehandlingType.REVURDERING.equals(
                b.getType()));
        //Strengere versjon var behandling = behandlingRepository.finnSisteInnvilgetBehandling(fagsakId).orElse(null);
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElse(null);
        if (finnesÅpneBehandlingerAvType || behandling == null) {
            return false;
        }
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class,
            behandling.getFagsakYtelseType()).orElseThrow();
        return revurderingTjeneste.kanRevurderingOpprettes(behandling.getFagsak());
    }

    private boolean kanOppretteFørstegangsbehandling(Long fagsakId) {
        var finnesÅpneBehandlingerAvType = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsakId)
            .stream()
            .anyMatch(b -> BehandlingType.FØRSTEGANGSSØKNAD.equals(b.getType()) || BehandlingType.REVURDERING.equals(
                b.getType()));
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElse(null);
        var sisteIkkeHenlagteBehandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsakId)
            .orElse(null);
        if (finnesÅpneBehandlingerAvType || sisteBehandling == null) {
            return false;
        }
        if (sisteIkkeHenlagteBehandling == null || FagsakYtelseType.ENGANGSTØNAD.equals(
            sisteIkkeHenlagteBehandling.getFagsakYtelseType())) {
            return true;
        }
        return Set.of(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.OPPHØR)
            .contains(sisteBehandling.getBehandlingsresultat().getBehandlingResultatType());
    }

    private MottattDokument finnSisteMottatteSøknadPåFagsak(Long fagsakId) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId)
            .stream()
            .filter(md -> md.getDokumentType().erSøknadType())
            // Søknader lagret fra utfylte papirsøknader skal ikke hentes, altså hvis de ikke er elektronisk
            // registert og har payLoadXml
            .filter(md -> md.getElektroniskRegistrert() || md.getPayloadXml() == null)
            .max(Comparator.comparing(MottattDokument::getMottattDato)
                .thenComparing(MottattDokument::getOpprettetTidspunkt))
            .orElse(null);
    }

    private MottattDokument finnSisteMottatteInntektsmeldingPåFagsak(Long fagsakId) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId)
            .stream()
            .filter(md -> md.getDokumentType().erInntektsmelding())
            .max(Comparator.comparing(MottattDokument::getMottattDato)
                .thenComparing(MottattDokument::getOpprettetTidspunkt))
            .orElse(null);
    }

    private boolean erLovÅOppretteNyBehandling(List<Behandling> behandlinger) {
        var ingenApenYtelsesBeh = behandlinger.stream()
            .noneMatch(b -> b.getType().equals(BehandlingType.REVURDERING) && !b.erAvsluttet()
                || b.getType().equals(BehandlingType.FØRSTEGANGSSØKNAD) && !b.erAvsluttet());

        var minstEnAvsluttet = behandlinger.stream().anyMatch(Behandling::erAvsluttet);

        return ingenApenYtelsesBeh && minstEnAvsluttet;
    }

    private boolean harMinstEnÅpenFørstegangsbehandling(List<Behandling> behandlinger) {
        return behandlinger.stream().anyMatch(b -> b.getType().equals(BehandlingType.FØRSTEGANGSSØKNAD) && !b.erAvsluttet());
    }

    private boolean erEtterKlageGyldigValg(Long fagsakId) {
        var klage = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsakId, BehandlingType.KLAGE)
            .orElse(null);

        // Vurdere å differensiere på KlageVurderingResultat (er tilstede) eller henlagt (resultat ikke tilstede)
        return klage != null && klage.erAvsluttet();
    }

    private static FunksjonellException kanIkkeOppretteRevurdering(Saksnummer saksnummer) {
        var msg = String.format("Fagsak med saksnummer %s oppfyller ikke kravene for revurdering", saksnummer);
        return new FunksjonellException("FP-663487", msg);
    }

    private static FunksjonellException kanIkkeOppretteNyFørstegangsbehandling(Long fagsakId) {
        return new FunksjonellException("FP-909861", "Det eksisterer allerede en åpen ytelsesbehandling eller det"
            + " eksisterer ingen avsluttede behandlinger for fagsakId " + fagsakId);
    }

    private static FunksjonellException kanIkkeHenleggeÅpenBehandlingOgOppretteNyFørstegangsbehandling(Long fagsakId) {
        return new FunksjonellException("FP-102451",
            "Det finnes ikke en åpen ytelsesbehandling som kan henlegges " + "for fagsakId " + fagsakId);
    }

    private static FunksjonellException kanIkkeOppretteNyFørstegangsbehandlingEtterKlage(Long fagsakId) {
        return new FunksjonellException("FP-909862",
            "Det eksisterer ingen avsluttede klagebehandlinger " + "for fagsakId " + fagsakId);
    }

    private static FunksjonellException ingenSøknadEllerImÅOppretteNyFørstegangsbehandlingPå(Long fagsakId) {
        var msg = String.format("FagsakId %s har ingen mottatte søknader eller "
            + "inntektsmeldinger som kan brukes til å opprette ny førstegangsbehandling", fagsakId);
        return new FunksjonellException("FP-287882", msg);
    }

}
