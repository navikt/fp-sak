package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
@FagsakYtelseTypeRef("SVP")
@DokumentGruppeRef("SØKNAD")
public class DokumentmottakerSøknadDefault extends DokumentmottakerSøknad {

    @Inject
    public DokumentmottakerSøknadDefault(BehandlingRepositoryProvider repositoryProvider,
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
}
