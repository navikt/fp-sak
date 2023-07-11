package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsperiodeVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelYtelse;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeGrunnlag;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class OpptjeningsperiodeVilkårTjenesteImpl implements OpptjeningsperiodeVilkårTjeneste {

    @Inject
    public OpptjeningsperiodeVilkårTjenesteImpl() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Override
    public VilkårData vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse) {
        var førsteUttaksdato = behandlingReferanse.getSkjæringstidspunkt().getFørsteUttaksdato();
        var grunnlag = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.SVANGERSKAP, RegelSøkerRolle.MORA, null).medFørsteUttaksDato(førsteUttaksdato);

        var resultat = InngangsvilkårRegler.opptjeningsperiode(RegelYtelse.SVANGERSKAPSPENGER, grunnlag);

        return RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSPERIODEVILKÅR, resultat);
    }
}
