package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;

public abstract class DokumentmottakerSøknad extends DokumentmottakerYtelsesesrelatertDokument {

    private static final Logger logger = LoggerFactory.getLogger(DokumentmottakerSøknad.class);

    private KøKontroller køKontroller;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    public DokumentmottakerSøknad(BehandlingRepositoryProvider repositoryProvider,
                                  DokumentmottakerFelles dokumentmottakerFelles,
                                  MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                  Behandlingsoppretter behandlingsoppretter,
                                  Kompletthetskontroller kompletthetskontroller,
                                  KøKontroller køKontroller,
                                  ForeldrepengerUttakTjeneste fpUttakTjeneste) {
        super(dokumentmottakerFelles,
            mottatteDokumentTjeneste,
            behandlingsoppretter,
            kompletthetskontroller,
            fpUttakTjeneste,
            repositoryProvider);
        this.køKontroller = køKontroller;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#S1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.empty());
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument.getJournalpostId());
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { //#S8
            // Start ny førstegangsbehandling av søknad
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else { //#S9
            // Oppretter revurdering siden det allerede er gjennomført en førstegangsbehandling på fagsaken
            Behandling revurdering = dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        }
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument.getJournalpostId());

        Fagsak fagsak = behandling.getFagsak();

        if (harMottattSøknadTidligere(behandling.getId())) { //#S2
            // Oppdatere behandling gjennom henleggelse
            Behandling nyBehandling = dokumentmottakerFelles.oppdatereViaHenleggelse(behandling, mottattDokument, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            Optional<Behandling> køetBehandlingMedforelder = revurderingRepository.finnKøetBehandlingMedforelder(fagsak);
            if (køetBehandlingMedforelder.map(Behandling::erRevurdering).orElse(false)) {
                køKontroller.dekøFørsteBehandlingISakskompleks(nyBehandling);
            } else {
                dokumentmottakerFelles.opprettTaskForÅStarteBehandling(nyBehandling);
            }
        } else {
            if (!mottattDokument.getElektroniskRegistrert()) { //#S3a
                if (kompletthetskontroller.støtterBehandlingstypePapirsøknad(behandling)) {
                    mottatteDokumentTjeneste.oppdaterMottattDokumentMedBehandling(mottattDokument, behandling.getId());
                    kompletthetskontroller.flyttTilbakeTilRegistreringPapirsøknad(behandling);
                } else { //#S3b
                    dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, behandling, mottattDokument);
                }
            } else { //#S3c
                kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
            }
        }
    }

    @Override
    public void håndterKøetBehandling(MottattDokument mottattDokument, Behandling køetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (harMottattSøknadTidligere(køetBehandling.getId())) { //#S13
            // Oppdatere behandling gjennom henleggelse
            Behandling nyKøetBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(køetBehandling, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            behandlingsoppretter.settSomKøet(nyKøetBehandling);
            Optional<LocalDate> søknadsdato = revurderingRepository.finnSøknadsdatoFraHenlagtBehandling(nyKøetBehandling);
            kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(nyKøetBehandling, mottattDokument, søknadsdato);
        } else { //#S10, #S11, #S12 og #S14
            // Oppdater køet behandling med søknad
            Optional<LocalDate> søknadsdato = Optional.empty();
            kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(køetBehandling, mottattDokument, søknadsdato);
        }
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (erAvslag(avsluttetBehandling)) { //#S4
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else if (harAvslåttPeriode(avsluttetBehandling) && behandlingsoppretter.harBehandlingsresultatOpphørt(avsluttetBehandling)) { //#S5
            Behandling revurdering = dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        } else if (BehandlingÅrsakType.ETTER_KLAGE.equals(behandlingÅrsakType)) { //#S6
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else { //#S7
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    @Override
    public void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak, Long avsluttetMedSøknadBehandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType, boolean opprettSomKøet) {
        if (opprettSomKøet) {
            // Ikke støttet og antagelig ikke ønsket interaktivt. Hvis i kø pga berørt på samme sak - da kan man vente. Kø pga medforelder skal behandles i INSØK, ikke i mottak!
            logger.warn("Ignorerer forsøk på å opprette ny førstegangsbehandling fra tidligere avsluttet id={} på fagsak={} da køing ikke er støttet her",
                avsluttetMedSøknadBehandlingId, fagsak.getId());
            return;
        }
        Behandling avsluttetBehandlingMedSøknad = behandlingRepository.hentBehandling(avsluttetMedSøknadBehandlingId);
        boolean harÅpenBehandling = !revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId()).map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.TRUE);
        if (harÅpenBehandling || erAvslag(avsluttetBehandlingMedSøknad) || avsluttetBehandlingMedSøknad.isBehandlingHenlagt()) {
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, avsluttetBehandlingMedSøknad, fagsak, behandlingÅrsakType);
        } else {
            Behandling revurdering = dokumentmottakerFelles.opprettManuellRevurdering(fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        }
    }

    protected void opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling behandling = behandlingsoppretter.opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(behandlingÅrsakType, fagsak);
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument.getJournalpostId());
    }

    protected void opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(MottattDokument mottattDokument, Behandling behandlingMedGrunnlag, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettNyFørstegangFraBehandlingMedSøknad(fagsak, behandlingÅrsakType, behandlingMedGrunnlag, mottattDokument);
    }

    @Override
    public boolean skalOppretteKøetBehandling(Fagsak fagsak) {
        return true;
    }

    @Override
    protected Behandling opprettKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());
        Behandling behandling;
        if (sisteYtelsesbehandling.isPresent() && harInnvilgetFørstegangsbehandling(sisteYtelsesbehandling.get())) {
            behandling = behandlingsoppretter.opprettRevurdering(fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else {
            behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.empty());
        }
        behandlingsoppretter.settSomKøet(behandling);
        return behandling;
    }

    private boolean harInnvilgetFørstegangsbehandling(Behandling behandling) {
        if (behandling.erRevurdering()) {
            behandling = finnFørstegangsbehandling(behandling);
        }
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        return behandlingsresultat.isPresent() && behandlingsresultat.get().isBehandlingsresultatInnvilget();
    }

    private Behandling finnFørstegangsbehandling(Behandling revurdering) {
        Optional<Behandling> behandling = revurdering.getOriginalBehandling();
        if (behandling.isPresent()) {
            if (!behandling.get().erRevurdering()) {
                return behandling.get();
            } else {
                return finnFørstegangsbehandling(behandling.get());
            }
        }
        throw new IllegalStateException("Utvikler-feil: Revurdering " + revurdering.getId() + " har ikke original behandling");
    }

    private BehandlingÅrsakType getBehandlingÅrsakHvisUdefinert(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsakType == null || BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType) ?
            BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER : behandlingÅrsakType;
    }

    protected boolean harMottattSøknadTidligere(Long behandlingId) {
        return mottatteDokumentTjeneste.harMottattDokumentSet(behandlingId, DokumentTypeId.getSøknadTyper()) ||
            mottatteDokumentTjeneste.harMottattDokumentSet(behandlingId, DokumentTypeId.getEndringSøknadTyper()) ||
            mottatteDokumentTjeneste.harMottattDokumentKat(behandlingId, DokumentKategori.SØKNAD);
    }
}
