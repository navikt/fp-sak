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
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("INNTEKTSMELDING")
class DokumentmottakerInntektsmelding extends DokumentmottakerYtelsesesrelatertDokument {

    private static final Logger logger = LoggerFactory.getLogger(DokumentmottakerInntektsmelding.class);

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentmottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           BehandlingRepositoryProvider repositoryProvider,
                                           ForeldrepengerUttakTjeneste fpUttakTjeneste) {
        super(dokumentmottakerFelles,
            mottatteDokumentTjeneste,
            behandlingsoppretter,
            kompletthetskontroller,
            fpUttakTjeneste,
            repositoryProvider);
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#I1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.empty());
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak.getId(), mottattDokument.getJournalpostId(), mottattDokument.getDokumentType());
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { //#I6
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
        } else { //#I7
            dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak.getId(), mottattDokument.getJournalpostId(), mottattDokument.getDokumentType());
        }
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#I2
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), mottattDokument.getDokumentType());
        dokumentmottakerFelles.leggTilBehandlingsårsak(behandling, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        dokumentmottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT);
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
    }

    @Override
    public void håndterKøetBehandling(MottattDokument mottattDokument, Behandling køetBehandling, BehandlingÅrsakType behandlingÅrsakType) { //#I8, #I9, #I10, #I11
        dokumentmottakerFelles.leggTilBehandlingsårsak(køetBehandling, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
        kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(køetBehandling, mottattDokument, Optional.empty());
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (fagsak.getYtelseType().gjelderEngangsstønad()) {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
            return;
        }
        if (dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(avsluttetBehandling.getFagsak())) { //#I3
            opprettNyFørstegangsbehandlingForMottattInntektsmelding(mottattDokument, fagsak, avsluttetBehandling);
        } else if (harAvslåttPeriode(avsluttetBehandling) && behandlingsoppretter.harBehandlingsresultatOpphørt(avsluttetBehandling)) { //#I4
            dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
            dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak.getId(), mottattDokument.getJournalpostId(), mottattDokument.getDokumentType());
        } else { //#I5
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    @Override
    public boolean skalOppretteKøetBehandling(Fagsak fagsak) {
        return true;
    }

    @Override
    protected Behandling opprettKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingsoppretter.opprettKøetBehandling(fagsak, getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType));
    }

    private BehandlingÅrsakType getBehandlingÅrsakHvisUdefinert(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsakType == null || BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType) ?
            BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING : behandlingÅrsakType;
    }

    @Override
    public void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak, Long behandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType, boolean opprettSomKøet) {
        if (opprettSomKøet) {
            // Ikke støttet og antagelig ikke ønsket interaktivt. Hvis i kø pga berørt på samme sak - da kan man vente. Kø pga medforelder skal behandles i INSØK, ikke i mottak!
            logger.warn("Ignorerer forsøk på å opprette ny førstegangsbehandling fra tidligere avsluttet id={} på fagsak={} da køing ikke er støttet her",
                behandlingId, fagsak.getId());
            return;
        }
        Behandling avsluttetBehandling = behandlingRepository.hentBehandling(behandlingId);
        boolean harÅpenBehandling = !revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId()).map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.TRUE);
        if (harÅpenBehandling || !(erAvslag(avsluttetBehandling) || avsluttetBehandling.isBehandlingHenlagt())) {
            logger.warn("Ignorerer forsøk på å opprette ny førstegangsbehandling fra tidligere avsluttet id={} på fagsak={}, der harÅpenBehandling={}, avsluttetHarAvslag={}, avsluttetErHenlagt={}",
                behandlingId, fagsak.getId(), harÅpenBehandling, erAvslag(avsluttetBehandling), avsluttetBehandling.isBehandlingHenlagt());
            return;
        }
        Behandling nyBehandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.of(avsluttetBehandling));
        mottatteDokumentTjeneste.persisterDokumentinnhold(nyBehandling, mottattDokument, Optional.empty());
        dokumentmottakerFelles.opprettHistorikk(nyBehandling, mottattDokument.getJournalpostId());
        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(nyBehandling);
    }

    private void opprettNyFørstegangsbehandlingForMottattInntektsmelding(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling) {
        dokumentmottakerFelles.opprettNyFørstegangFraAvslag(mottattDokument, fagsak, avsluttetBehandling);
    }
}
