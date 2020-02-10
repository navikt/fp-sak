package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.FaktaUttakAksjonspunktUtleder;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class GraderingAktivitetUtenBGAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {


    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        return input.finnesAndelerMedGraderingUtenBeregningsgrunnlag() ?
            List.of(AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG) : List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return true;
    }
}
