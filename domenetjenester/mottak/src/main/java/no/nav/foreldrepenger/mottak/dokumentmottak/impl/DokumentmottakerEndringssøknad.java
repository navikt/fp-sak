package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("ENDRINGSSØKNAD")
class DokumentmottakerEndringssøknad extends DokumentmottakerYtelsesesrelatertDokument {

    private KøKontroller køKontroller;

    @Inject
    public DokumentmottakerEndringssøknad(BehandlingRepositoryProvider repositoryProvider,
                                          DokumentmottakerFelles dokumentmottakerFelles,
                                          MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                          Behandlingsoppretter behandlingsoppretter,
                                          Kompletthetskontroller kompletthetskontroller,
                                          KøKontroller køKontroller) {
        super(dokumentmottakerFelles,
            mottatteDokumentTjeneste,
            behandlingsoppretter,
            kompletthetskontroller,
            repositoryProvider);
        this.køKontroller = køKontroller;
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument.getJournalpostId());

        BehandlingÅrsakType brukÅrsakType = getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType);
        dokumentmottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(behandling, brukÅrsakType);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) { //#E2
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
        } else if (harAlleredeMottattEndringssøknad(behandling)) { //#E3
            Behandling nyBehandling = dokumentmottakerFelles.oppdatereViaHenleggelse(behandling, mottattDokument, brukÅrsakType);
            køKontroller.dekøFørsteBehandlingISakskompleks(nyBehandling);
        } else if (kompletthetErPassert(behandling)) { //#E4
            Behandling nyBehandling = dokumentmottakerFelles.oppdatereViaHenleggelse(behandling, mottattDokument, brukÅrsakType);
            dokumentmottakerFelles.opprettTaskForÅStarteBehandling(nyBehandling);
        } else { //#E5
            mottatteDokumentTjeneste.oppdaterMottattDokumentMedBehandling(mottattDokument, behandling.getId());
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
        } else if (harAlleredeMottattEndringssøknad(køetBehandling)) { //#E14
            // Oppdatere behandling gjennom henleggelse
            Behandling nyKøetBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(køetBehandling, BehandlingÅrsakType.KØET_BEHANDLING);
            behandlingsoppretter.settSomKøet(nyKøetBehandling);
            Optional<LocalDate> søknadsdato = revurderingRepository.finnSøknadsdatoFraHenlagtBehandling(nyKøetBehandling);
            kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(nyKøetBehandling, mottattDokument, søknadsdato);
        } else { //#E11, #E13 og #E15
            // Oppdater køet behandling med søknad
            Optional<LocalDate> søknadsdato = Optional.empty();
            dokumentmottakerFelles.leggTilBehandlingsårsak(køetBehandling, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(køetBehandling, mottattDokument, søknadsdato);
        }
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (fagsak.getYtelseType().gjelderEngangsstønad()) {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
            return;
        }
        if (dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(avsluttetBehandling.getFagsak())) { //#E6
            dokumentmottakerFelles.opprettNyFørstegangFraAvslag(mottattDokument, fagsak, avsluttetBehandling);
        } else if (harAvslåttPeriode(avsluttetBehandling) && behandlingsoppretter.harBehandlingsresultatOpphørt(avsluttetBehandling)) { //#E7
            Behandling revurdering = dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        } else { //#E8
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { //#E9
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
        } else { //#E10
            Behandling revurdering = dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        }
    }

    @Override
    public boolean skalOppretteKøetBehandling(Fagsak fagsak) {
        return !behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak);
    }

    @Override
    protected Behandling opprettKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingsoppretter.opprettKøetBehandling(fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        // Kan ikke håndtere endringssøknad når ingen behandling finnes -> Opprett manuell task
        dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); //#E1
    }

    private BehandlingÅrsakType getBehandlingÅrsakHvisUdefinert(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsakType == null || BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType) ?
            BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER : behandlingÅrsakType;
    }

    private boolean kompletthetErPassert(Behandling behandling) {
        return behandlingsoppretter.erKompletthetssjekkPassert(behandling);
    }

    private boolean harAlleredeMottattEndringssøknad(Behandling behandling) {
        return mottatteDokumentTjeneste.harMottattDokumentSet(behandling.getId(), DokumentTypeId.getEndringSøknadTyper());
    }
}
