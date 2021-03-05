package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

public abstract class DokumentmottakerSøknad extends DokumentmottakerYtelsesesrelatertDokument {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentmottakerSøknad.class);

    private KøKontroller køKontroller;

    public DokumentmottakerSøknad(BehandlingRepositoryProvider repositoryProvider,
                                  DokumentmottakerFelles dokumentmottakerFelles,
                                  Behandlingsoppretter behandlingsoppretter,
                                  Kompletthetskontroller kompletthetskontroller,
                                  KøKontroller køKontroller,
                                  ForeldrepengerUttakTjeneste fpUttakTjeneste) {
        super(dokumentmottakerFelles,
            behandlingsoppretter,
            kompletthetskontroller,
            fpUttakTjeneste,
            repositoryProvider);
        this.køKontroller = køKontroller;
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument);

        Fagsak fagsak = behandling.getFagsak();

        if (dokumentmottakerFelles.harMottattSøknadTidligere(behandling.getId())) { //#S2
            // Oppdatere behandling gjennom henleggelse
            Behandling nyBehandling = dokumentmottakerFelles.oppdatereViaHenleggelse(behandling, mottattDokument, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            Optional<Behandling> køetBehandlingMedforelder = revurderingRepository.finnKøetBehandlingMedforelder(fagsak).filter(Behandling::erRevurdering);
            if (køetBehandlingMedforelder.isPresent()) {
                køKontroller.dekøFørsteBehandlingISakskompleks(nyBehandling);
            } else {
                dokumentmottakerFelles.opprettTaskForÅStarteBehandling(nyBehandling);
            }
        } else {
            if (!mottattDokument.getElektroniskRegistrert()) { //#S3a
                if (kompletthetskontroller.støtterBehandlingstypePapirsøknad(behandling)) {
                    dokumentmottakerFelles.oppdaterMottattDokumentMedBehandling(mottattDokument, behandling.getId());
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
        if (dokumentmottakerFelles.harMottattSøknadTidligere(køetBehandling.getId())) { //#S13
            // Oppdatere behandling gjennom henleggelse
            dokumentmottakerFelles.oppdatereViaHenleggelseEnkø(køetBehandling, mottattDokument, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else { //#S10, #S11, #S12 og #S14
            // Oppdater køet behandling med søknad
            Optional<LocalDate> søknadsdato = Optional.empty();
            kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(køetBehandling, mottattDokument, søknadsdato);
        }
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#S1
        dokumentmottakerFelles.opprettInitiellFørstegangsbehandling(fagsak, mottattDokument, behandlingÅrsakType);
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { //#S8
            // Start ny førstegangsbehandling av søknad
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else { //#S9
            // Oppretter revurdering siden det allerede er gjennomført en førstegangsbehandling på fagsaken
            dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        }
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (erAvslag(avsluttetBehandling)) { //#S4 #S6
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else { // diverse
            dokumentmottakerFelles.standardForAvslåttEllerOpphørtBehandling(mottattDokument, fagsak, avsluttetBehandling,
                getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType), harAvslåttPeriode(avsluttetBehandling));
        }
    }

    @Override
    public boolean skalOppretteKøetBehandling(Fagsak fagsak) {
        return true;
    }

    @Override
    protected void opprettKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling sisteAvsluttetBehandling) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak) || sisteAvsluttetBehandling == null || erAvslag(sisteAvsluttetBehandling)) {
            dokumentmottakerFelles.opprettKøetFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else {
            dokumentmottakerFelles.opprettKøetRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        }
    }

    @Override
    public void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak, Long avsluttetMedSøknadBehandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType, boolean opprettSomKøet) {
        Behandling avsluttetBehandlingMedSøknad = behandlingRepository.hentBehandling(avsluttetMedSøknadBehandlingId);
        boolean harÅpenBehandling = !revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId()).map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.TRUE);
        if (harÅpenBehandling || erAvslag(avsluttetBehandlingMedSøknad) || avsluttetBehandlingMedSøknad.isBehandlingHenlagt()) {
            dokumentmottakerFelles.opprettNyFørstegangFraBehandlingMedSøknad(fagsak, behandlingÅrsakType, avsluttetBehandlingMedSøknad, mottattDokument, opprettSomKøet);
        } else {
            Behandling revurdering = dokumentmottakerFelles.opprettManuellRevurdering(fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType), opprettSomKøet);
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument);
        }
    }

    protected BehandlingÅrsakType getBehandlingÅrsakHvisUdefinert(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsakType == null || BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType) ?
            BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER : behandlingÅrsakType;
    }
}
