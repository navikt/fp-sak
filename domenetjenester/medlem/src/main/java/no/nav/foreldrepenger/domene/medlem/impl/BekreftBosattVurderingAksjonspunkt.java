package no.nav.foreldrepenger.domene.medlem.impl;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.medlem.api.BekreftBosattVurderingAksjonspunktDto;

public class BekreftBosattVurderingAksjonspunkt {

    private MedlemskapRepository medlemskapRepository;

    public BekreftBosattVurderingAksjonspunkt(BehandlingRepositoryProvider repositoryProvider) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    public void oppdater(Long behandlingId, BekreftBosattVurderingAksjonspunktDto adapter) {
        Optional<VurdertMedlemskap> vurdertMedlemskap = medlemskapRepository.hentVurdertMedlemskap(behandlingId);

        VurdertMedlemskap nytt = new VurdertMedlemskapBuilder(vurdertMedlemskap)
            .medBosattVurdering(adapter.getBosattVurdering())
            .medBegrunnelse(adapter.getBegrunnelse())
            .build();

        medlemskapRepository.lagreMedlemskapVurdering(behandlingId, nytt);
    }
}
