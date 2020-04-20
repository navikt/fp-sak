package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
@FagsakYtelseTypeRef("SVP")
@DokumentGruppeRef("SØKNAD")
public class DokumentmottakerSøknadDefault extends DokumentmottakerSøknad {

    @Inject
    public DokumentmottakerSøknadDefault(BehandlingRepositoryProvider repositoryProvider,
                                         DokumentmottakerFelles dokumentmottakerFelles,
                                         Behandlingsoppretter behandlingsoppretter,
                                         Kompletthetskontroller kompletthetskontroller,
                                         KøKontroller køKontroller,
                                         ForeldrepengerUttakTjeneste fpUttakTjeneste) {
        super(repositoryProvider,
            dokumentmottakerFelles,
            behandlingsoppretter,
            kompletthetskontroller,
            køKontroller,
            fpUttakTjeneste);
    }
}
