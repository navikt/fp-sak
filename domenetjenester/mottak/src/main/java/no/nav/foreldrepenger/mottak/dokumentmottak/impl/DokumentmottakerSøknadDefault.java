package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@DokumentGruppeRef(DokumentGruppe.SØKNAD)
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
