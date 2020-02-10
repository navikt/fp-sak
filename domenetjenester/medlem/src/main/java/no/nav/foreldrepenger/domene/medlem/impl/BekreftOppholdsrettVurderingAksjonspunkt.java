package no.nav.foreldrepenger.domene.medlem.impl;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.medlem.api.BekreftOppholdVurderingAksjonspunktDto;

public class BekreftOppholdsrettVurderingAksjonspunkt {

    private MedlemskapRepository medlemskapRepository;

    public BekreftOppholdsrettVurderingAksjonspunkt(BehandlingRepositoryProvider repositoryProvider) {
        medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    public void oppdater(Long behandlingId, BekreftOppholdVurderingAksjonspunktDto adapter) {
        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        Optional<VurdertMedlemskap> vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);

        VurdertMedlemskap nytt = new VurdertMedlemskapBuilder(vurdertMedlemskap)
            .medOppholdsrettVurdering(adapter.getOppholdsrettVurdering())
            .medLovligOppholdVurdering(adapter.getLovligOppholdVurdering())
            .medErEosBorger(adapter.getErEosBorger())
            .medBegrunnelse(adapter.getBegrunnelse())
            .build();

        medlemskapRepository.lagreMedlemskapVurdering(behandlingId, nytt);
    }
}
