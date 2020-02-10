package no.nav.foreldrepenger.mottak.dokumentmottak.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentGruppeRef;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerFelles;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerSøknad;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Kompletthetskontroller;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;

@ApplicationScoped
@FagsakYtelseTypeRef("ES")
@DokumentGruppeRef("SØKNAD")
public class DokumentmottakerSøknadEngangsstønad extends DokumentmottakerSøknad {

    @Inject
    public DokumentmottakerSøknadEngangsstønad(BehandlingRepositoryProvider repositoryProvider,
                                    DokumentmottakerFelles dokumentmottakerFelles,
                                    MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                    Behandlingsoppretter behandlingsoppretter,
                                    Kompletthetskontroller kompletthetskontroller,
                                    KøKontroller køKontroller) {
        super(repositoryProvider,
            dokumentmottakerFelles,
            mottatteDokumentTjeneste,
            behandlingsoppretter,
            kompletthetskontroller,
            køKontroller);
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (erAvslag(avsluttetBehandling) || BehandlingÅrsakType.ETTER_KLAGE.equals(behandlingÅrsakType)) {
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType); //#SE1
        } else {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument); //#SE2
        }
    }

    @Override
    public void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak, Long avsluttetMedSøknadBehandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType, boolean opprettSomKøet) { //SExx
        Behandling avsluttetBehandlingMedSøknad = behandlingRepository.hentBehandling(avsluttetMedSøknadBehandlingId);
        opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, avsluttetBehandlingMedSøknad, fagsak, behandlingÅrsakType);
    }
}
