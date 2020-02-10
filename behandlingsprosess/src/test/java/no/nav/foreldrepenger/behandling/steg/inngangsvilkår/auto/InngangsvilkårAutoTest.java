package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.auto;


import static org.hamcrest.CoreMatchers.equalTo;

import java.time.Period;
import java.util.function.BiConsumer;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;

// TODO: Skrive om til @RunWith(Parameterized.class) hvor det hentes opp object med input / output sammen
@Ignore("Feiler periodisk, skal skrives om")
public class InngangsvilkårAutoTest {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void vurderMedlemskap() {
        new VilkårVurdering().vurderVilkår(collector, VilkårType.MEDLEMSKAPSVILKÅRET);
    }

    @Test
    public void vurderFødselMor() {
        new VilkårVurdering().vurderVilkår(collector, VilkårType.FØDSELSVILKÅRET_MOR);
    }

    @Test
    public void vurderFødselFarMedMor() {
        new VilkårVurdering().vurderVilkår(collector, VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
    }

    @Test
    public void vurderAdopsjonEngangsstønad() {
        new VilkårVurdering().vurderVilkår(collector, VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
    }

    @Test
    public void vurderAdopsjonForeldrepenger() {
        new VilkårVurdering().vurderVilkår(collector, VilkårType.ADOPSJONSVILKARET_FORELDREPENGER);
    }

    @Test
    public void vurderOpptjening() {
        final BiConsumer<VilkårResultat, Object> extraDataSjekk = (resultat, extra) ->
            collector.checkThat("Avvik i opptjeningstid for " + resultat,
                ((OpptjeningsvilkårResultat) extra).getResultatOpptjent(), equalTo(Period.parse(resultat.getOpptjentTid())));
        new VilkårVurdering().vurderVilkår(collector, VilkårType.OPPTJENINGSVILKÅRET, extraDataSjekk);
    }

    @Test
    public void fastsettOpptjeningsPeriode() {
        final BiConsumer<VilkårResultat, Object> extraDataSjekk = (resultat, extra) -> {
            collector.checkThat("Avvik i opptjeningstid for " + resultat,
                ((OpptjeningsPeriode) extra).getOpptjeningsperiodeFom(), equalTo(resultat.getOpptjeningFom()));
            collector.checkThat("Avvik i opptjeningstid for " + resultat,
                ((OpptjeningsPeriode) extra).getOpptjeningsperiodeTom(), equalTo(resultat.getOpptjeningTom()));
        };
        new VilkårVurdering().vurderVilkår(collector, VilkårType.OPPTJENINGSPERIODEVILKÅR, extraDataSjekk);
    }
}
