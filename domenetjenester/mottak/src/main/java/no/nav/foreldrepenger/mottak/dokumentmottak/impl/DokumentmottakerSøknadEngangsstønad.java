package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@DokumentGruppeRef(DokumentGruppe.SØKNAD)
public class DokumentmottakerSøknadEngangsstønad extends DokumentmottakerSøknad {

    @Inject
    public DokumentmottakerSøknadEngangsstønad(BehandlingRepository behandlingRepository,
                                               DokumentmottakerFelles dokumentmottakerFelles,
                                               Behandlingsoppretter behandlingsoppretter,
                                               Kompletthetskontroller kompletthetskontroller,
                                               KøKontroller køKontroller,
                                               ForeldrepengerUttakTjeneste fpUttakTjeneste,
                                               BehandlingRevurderingTjeneste behandlingRevurderingTjeneste) {
        super(behandlingRepository, dokumentmottakerFelles, behandlingsoppretter, kompletthetskontroller, køKontroller, fpUttakTjeneste,
            behandlingRevurderingTjeneste);
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument,
                                                     Fagsak fagsak,
                                                     Behandling avsluttetBehandling,
                                                     BehandlingÅrsakType behandlingÅrsakType) {
        if (erAvslag(avsluttetBehandling) || BehandlingÅrsakType.ETTER_KLAGE.equals(behandlingÅrsakType)) {
            dokumentmottakerFelles.opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak,
                getBehandlingÅrsakHvisUdefinert(behandlingÅrsakType)); //#SE1
        } else {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument); //#SE2
        }
    }

    @Override
    public void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak,
                                                       Long avsluttetMedSøknadBehandlingId,
                                                       MottattDokument mottattDokument,
                                                       BehandlingÅrsakType behandlingÅrsakType,
                                                       boolean opprettSomKøet) { //SExx
        var avsluttetBehandlingMedSøknad = behandlingRepository.hentBehandling(avsluttetMedSøknadBehandlingId);
        dokumentmottakerFelles.opprettNyFørstegangFraBehandlingMedSøknad(fagsak, behandlingÅrsakType, avsluttetBehandlingMedSøknad, mottattDokument);
    }
}
