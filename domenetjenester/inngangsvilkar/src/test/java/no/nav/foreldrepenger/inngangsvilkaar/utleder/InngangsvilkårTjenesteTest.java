package no.nav.foreldrepenger.inngangsvilkaar.utleder;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;

@CdiDbAwareTest
class InngangsvilkårTjenesteTest {

    @Inject
    InngangsvilkårTjeneste inngangsvilkårTjeneste;

    @Test
    void skal_slå_opp_inngangsvilkår() {
        sjekkVilkårKonfigurasjon(VilkårType.FØDSELSVILKÅRET_MOR, FagsakYtelseType.FORELDREPENGER, false);
        sjekkVilkårKonfigurasjon(VilkårType.MEDLEMSKAPSVILKÅRET, FagsakYtelseType.FORELDREPENGER, false);
        sjekkVilkårKonfigurasjon(VilkårType.SØKNADSFRISTVILKÅRET, FagsakYtelseType.FORELDREPENGER, false);
        sjekkVilkårKonfigurasjon(VilkårType.OMSORGSOVERTAKELSEVILKÅR, FagsakYtelseType.FORELDREPENGER, false);
    }

    @Test
    void skal_slå_opp_inngangsvilkår_meg_fagsak_ytelse_type_der_inngangsvilkåret_er_forskjellig_pr_ytelse() {
        sjekkVilkårKonfigurasjon(VilkårType.OPPTJENINGSPERIODEVILKÅR, FagsakYtelseType.SVANGERSKAPSPENGER, true);
        sjekkVilkårKonfigurasjon(VilkårType.OPPTJENINGSPERIODEVILKÅR, FagsakYtelseType.FORELDREPENGER, true);
    }

    private void sjekkVilkårKonfigurasjon(VilkårType vilkårType, FagsakYtelseType foreldrepenger, boolean sjekkForFagYtelseType) {
        var vilkår = inngangsvilkårTjeneste.finnVilkår(vilkårType, foreldrepenger);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår).isSameAs(inngangsvilkårTjeneste.finnVilkår(vilkårType, foreldrepenger));
        assertThat(vilkår.getClass()).hasAnnotation(ApplicationScoped.class);
        assertThat(vilkår.getClass()).hasAnnotation(VilkårTypeRef.class);
        if (sjekkForFagYtelseType) {
            assertThat(vilkår.getClass()).hasAnnotation(FagsakYtelseTypeRef.class);
        }
    }
}
