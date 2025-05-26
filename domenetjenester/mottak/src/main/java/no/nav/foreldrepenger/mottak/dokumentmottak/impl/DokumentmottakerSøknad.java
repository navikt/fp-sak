package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;

public abstract class DokumentmottakerSøknad extends DokumentmottakerYtelsesesrelatertDokument {

    private final KøKontroller køKontroller;

    public DokumentmottakerSøknad(BehandlingRepository behandlingRepository,
                                  DokumentmottakerFelles dokumentmottakerFelles,
                                  Behandlingsoppretter behandlingsoppretter,
                                  Kompletthetskontroller kompletthetskontroller,
                                  KøKontroller køKontroller,
                                  ForeldrepengerUttakTjeneste fpUttakTjeneste,
                                  BehandlingRevurderingTjeneste behandlingRevurderingTjeneste) {
        super(dokumentmottakerFelles,
            behandlingsoppretter,
            kompletthetskontroller,
            fpUttakTjeneste,
                behandlingRevurderingTjeneste,
            behandlingRepository);
        this.køKontroller = køKontroller;
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        var fagsak = behandling.getFagsak();

        if (dokumentmottakerFelles.harMottattSøknadTidligere(behandling.getId())) { //#S2
            // Oppdatere behandling gjennom henleggelse
            var nyBehandling = dokumentmottakerFelles.oppdatereViaHenleggelse(behandling, mottattDokument, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            køKontroller.dekøFørsteBehandlingISakskompleks(nyBehandling);
        } else {
            dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument);
            if (behandling.erRevurdering()) {
                dokumentmottakerFelles.leggTilBehandlingsårsak(behandling, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            }
            if (!mottattDokument.getElektroniskRegistrert()) { //#S3a
                if (kompletthetskontroller.støtterBehandlingstypePapirsøknad(behandling)) {
                    var lås = behandlingRepository.taSkriveLås(behandling.getId());
                    dokumentmottakerFelles.oppdaterMottattDokumentMedBehandling(mottattDokument, behandling.getId());
                    kompletthetskontroller.flyttTilbakeTilRegistreringPapirsøknad(behandling, lås);
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
            if (BehandlingType.FØRSTEGANGSSØKNAD.equals(køetBehandling.getType())) {
                dokumentmottakerFelles.oppdatereViaHenleggelse(køetBehandling, mottattDokument, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            } else {
                dokumentmottakerFelles.oppdatereViaHenleggelseEnkø(køetBehandling, mottattDokument, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            }
        } else { //#S10, #S11, #S12 og #S14
            // Oppdater køet behandling med søknad
            if (køetBehandling.erRevurdering()) {
                dokumentmottakerFelles.leggTilBehandlingsårsak(køetBehandling, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            }
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
    public void håndterUtsattStartdato(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (erAvslag(avsluttetBehandling)) { //#S4 #S6
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else if (erOpphør(avsluttetBehandling) && !harInnvilgetPeriode(avsluttetBehandling)) {
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else { // diverse
            dokumentmottakerFelles.standardForAvslåttEllerOpphørtBehandling(mottattDokument, fagsak, avsluttetBehandling,
                getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType), harInnvilgetPeriode(avsluttetBehandling));
        }
    }

    @Override
    public boolean skalOppretteKøetBehandling(Fagsak fagsak) {
        return true;
    }

    @Override
    protected void opprettKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling sisteAvsluttetBehandling) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak) || sisteAvsluttetBehandling == null || erAvslag(sisteAvsluttetBehandling)) {
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else {
            dokumentmottakerFelles.opprettKøetRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        }
    }

    @Override
    public void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak, Long avsluttetMedSøknadBehandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType, boolean opprettSomKøet) {
        var avsluttetBehandlingMedSøknad = behandlingRepository.hentBehandling(avsluttetMedSøknadBehandlingId);
        var harÅpenBehandling = !behandlingRevurderingTjeneste.hentAktivIkkeBerørtEllerSisteYtelsesbehandling(fagsak.getId()).map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.TRUE);
        if (harÅpenBehandling || erAvslag(avsluttetBehandlingMedSøknad) || dokumentmottakerFelles.erBehandlingHenlagt(avsluttetBehandlingMedSøknad.getId())) {
            dokumentmottakerFelles.opprettNyFørstegangFraBehandlingMedSøknad(fagsak, behandlingÅrsakType, avsluttetBehandlingMedSøknad, mottattDokument);
        } else {
            var revurdering = dokumentmottakerFelles.opprettManuellRevurdering(fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType), opprettSomKøet);
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument);
        }
    }

    protected BehandlingÅrsakType getBehandlingÅrsakHvisUdefinert(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsakType == null || BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType) ?
            BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER : behandlingÅrsakType;
    }
}
