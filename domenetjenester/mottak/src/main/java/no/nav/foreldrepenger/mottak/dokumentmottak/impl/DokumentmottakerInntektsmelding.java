package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("INNTEKTSMELDING")
class DokumentmottakerInntektsmelding extends DokumentmottakerYtelsesesrelatertDokument {

    private static final Logger logger = LoggerFactory.getLogger(DokumentmottakerInntektsmelding.class);

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentmottakerFelles,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           BehandlingRepositoryProvider repositoryProvider,
                                           ForeldrepengerUttakTjeneste fpUttakTjeneste) {
        super(dokumentmottakerFelles,
            behandlingsoppretter,
            kompletthetskontroller,
            fpUttakTjeneste,
            repositoryProvider);
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#I2
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument);
        dokumentmottakerFelles.leggTilBehandlingsårsak(behandling, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
    }

    @Override
    public void håndterKøetBehandling(MottattDokument mottattDokument, Behandling køetBehandling, BehandlingÅrsakType behandlingÅrsakType) { //#I8, #I9, #I10, #I11
        dokumentmottakerFelles.leggTilBehandlingsårsak(køetBehandling, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(køetBehandling, mottattDokument, Optional.empty());
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#I1
        dokumentmottakerFelles.opprettInitiellFørstegangsbehandling(fagsak, mottattDokument, behandlingÅrsakType);
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { //#I6
            if (dokumentmottakerFelles.harFagsakMottattSøknadTidligere(fagsak.getId())) {
                dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
            } else {
                dokumentmottakerFelles.opprettInitiellFørstegangsbehandling(fagsak, mottattDokument,getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            }
        } else { //#I7
            dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        }
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.standardForAvslåttEllerOpphørtBehandling(mottattDokument, fagsak, avsluttetBehandling,
            getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType), harAvslåttPeriode(avsluttetBehandling));
    }

    @Override
    public boolean skalOppretteKøetBehandling(Fagsak fagsak) {
        return true;
    }

    @Override
    protected void opprettKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling sisteAvsluttetBehandling) {
        if (sisteAvsluttetBehandling != null && dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(sisteAvsluttetBehandling.getFagsak())) { //#I3 #E6
            dokumentmottakerFelles.opprettKøetFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        } else if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak) || sisteAvsluttetBehandling == null || erAvslag(sisteAvsluttetBehandling)) { //#E9
            if (dokumentmottakerFelles.harFagsakMottattSøknadTidligere(fagsak.getId())) {
                dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
            } else {
                // Informasjonssak, potensielt autohenlagt Inntektsmelding
                dokumentmottakerFelles.opprettKøetInitiellFørstegangsbehandling(fagsak, mottattDokument,getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            }
        } else { //#E10
            dokumentmottakerFelles.opprettKøetRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        }
    }

    private BehandlingÅrsakType getBehandlingÅrsakHvisUdefinert(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsakType == null || BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType) ?
            BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING : behandlingÅrsakType;
    }

    @Override
    public void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak, Long behandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType, boolean opprettSomKøet) {
        Behandling avsluttetBehandling = behandlingRepository.hentBehandling(behandlingId);
        boolean harÅpenBehandling = !revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId()).map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.TRUE);
        if (harÅpenBehandling || !(erAvslag(avsluttetBehandling) || avsluttetBehandling.isBehandlingHenlagt())) {
            logger.warn("Ignorerer forsøk på å opprette ny førstegangsbehandling fra tidligere avsluttet id={} på fagsak={}, der harÅpenBehandling={}, avsluttetHarAvslag={}, avsluttetErHenlagt={}",
                behandlingId, fagsak.getId(), harÅpenBehandling, erAvslag(avsluttetBehandling), avsluttetBehandling.isBehandlingHenlagt());
            return;
        }
        Behandling nyBehandling = dokumentmottakerFelles.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.of(avsluttetBehandling), opprettSomKøet);
        dokumentmottakerFelles.persisterDokumentinnhold(nyBehandling, mottattDokument);
        dokumentmottakerFelles.opprettHistorikk(nyBehandling, mottattDokument);
        if (!opprettSomKøet) {
            dokumentmottakerFelles.opprettTaskForÅStarteBehandling(nyBehandling);
        }
    }
}
