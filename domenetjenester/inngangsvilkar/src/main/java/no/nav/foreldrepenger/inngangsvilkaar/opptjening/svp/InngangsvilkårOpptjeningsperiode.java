package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelYtelse;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@VilkårTypeRef(VilkårType.OPPTJENINGSPERIODEVILKÅR)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class InngangsvilkårOpptjeningsperiode implements Inngangsvilkår {

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    InngangsvilkårOpptjeningsperiode() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOpptjeningsperiode(SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId());
        var førsteUttaksdato = stp.getFørsteUttaksdato();
        var grunnlag = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.SVANGERSKAP, RegelSøkerRolle.MORA, null).medFørsteUttaksDato(førsteUttaksdato);

        var resultat = InngangsvilkårRegler.opptjeningsperiode(RegelYtelse.SVANGERSKAPSPENGER, grunnlag);

        return RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSPERIODEVILKÅR, resultat);
    }
}
