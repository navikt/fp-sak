package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import java.time.LocalDate;
import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsperiodeVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.svp.RegelFastsettOpptjeningsperiode;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class OpptjeningsperiodeVilkårTjenesteImpl implements OpptjeningsperiodeVilkårTjeneste {

    private InngangsvilkårOversetter inngangsvilkårOversetter;
    private Period antallDagerOpptjeningsperiode;

    OpptjeningsperiodeVilkårTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningsperiodeVilkårTjenesteImpl(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
        this.antallDagerOpptjeningsperiode = Period.ofDays(28);
    }

    @Override
    public VilkårData vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse, LocalDate førsteUttaksdato) {
        var grunnlag = new OpptjeningsperiodeGrunnlag();

        grunnlag.setFagsakÅrsak(FagsakÅrsak.SVANGERSKAP);
        grunnlag.setFørsteUttaksDato(førsteUttaksdato);
        grunnlag.setPeriodeLengde(antallDagerOpptjeningsperiode);
        grunnlag.setTidligsteUttakFørFødselPeriode(null);

        final var data = new OpptjeningsPeriode();
        var evaluation = new RegelFastsettOpptjeningsperiode().evaluer(grunnlag, data);

        var resultat = inngangsvilkårOversetter.tilVilkårData(VilkårType.OPPTJENINGSPERIODEVILKÅR, evaluation, grunnlag);
        resultat.setEkstraVilkårresultat(data);
        return resultat;
    }
}
