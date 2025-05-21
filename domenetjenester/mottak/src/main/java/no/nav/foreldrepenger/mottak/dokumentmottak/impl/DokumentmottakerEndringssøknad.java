package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(DokumentGruppe.ENDRINGSSØKNAD)
class DokumentmottakerEndringssøknad extends DokumentmottakerYtelsesesrelatertDokument {

    private final KøKontroller køKontroller;

    @Inject
    public DokumentmottakerEndringssøknad(BehandlingRepository behandlingRepository,
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
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument);

        var brukÅrsakType = getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) { //#E2
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
        } else if (dokumentmottakerFelles.harAlleredeMottattEndringssøknad(behandling) || kompletthetErPassert(behandling)) { //#E3 + #E4
            var nyBehandling = dokumentmottakerFelles.oppdatereViaHenleggelse(behandling, mottattDokument, brukÅrsakType);
            køKontroller.dekøFørsteBehandlingISakskompleks(nyBehandling);
        } else { //#E5
            dokumentmottakerFelles.oppdaterMottattDokumentMedBehandling(mottattDokument, behandling.getId());
            // Oppdater åpen behandling med Endringssøknad
            dokumentmottakerFelles.leggTilBehandlingsårsak(behandling, brukÅrsakType);
            if (!mottattDokument.getElektroniskRegistrert()) {
                kompletthetskontroller.flyttTilbakeTilRegistreringPapirsøknad(behandling);
                return;
            }
            kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        }
    }

    @Override
    public void håndterKøetBehandling(MottattDokument mottattDokument, Behandling køetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(køetBehandling.getType())) { //#E16
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(køetBehandling.getFagsak(), køetBehandling, mottattDokument);
        } else if (dokumentmottakerFelles.harAlleredeMottattEndringssøknad(køetBehandling)) { //#E14
            // Oppdatere behandling gjennom henleggelse
            dokumentmottakerFelles.oppdatereViaHenleggelseEnkø(køetBehandling, mottattDokument, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else { //#E11, #E13 og #E15
            // Oppdater køet behandling med søknad
            Optional<LocalDate> søknadsdato = Optional.empty();
            dokumentmottakerFelles.leggTilBehandlingsårsak(køetBehandling, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(køetBehandling, mottattDokument, søknadsdato);
        }
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        // Kan ikke håndtere endringssøknad når ingen behandling finnes -> Opprett manuell task
        dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); //#E1
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { //#E9
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
        } else { //#E10
            dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        }
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.standardForAvslåttEllerOpphørtBehandling(mottattDokument, fagsak, avsluttetBehandling,
            getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType), true);
    }

    @Override
    public void håndterUtsattStartdato(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Override
    public boolean skalOppretteKøetBehandling(Fagsak fagsak) {
        return !behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak);
    }

    @Override
    protected void opprettKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling sisteAvsluttetBehandling) {
        if (sisteAvsluttetBehandling != null && dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(sisteAvsluttetBehandling.getFagsak())) { //#I3 #E6
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak) || sisteAvsluttetBehandling == null || erAvslag(sisteAvsluttetBehandling)) { //#E9
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
        } else { //#E10
            dokumentmottakerFelles.opprettKøetRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        }
    }

    private BehandlingÅrsakType getBehandlingÅrsakHvisUdefinert(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsakType == null || BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType) ?
            BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER : behandlingÅrsakType;
    }

    private boolean kompletthetErPassert(Behandling behandling) {
        return kompletthetskontroller.erKompletthetssjekkPassert(behandling);
    }
}
