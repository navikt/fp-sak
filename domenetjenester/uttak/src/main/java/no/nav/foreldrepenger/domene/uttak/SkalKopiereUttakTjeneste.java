package no.nav.foreldrepenger.domene.uttak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@ApplicationScoped
public class SkalKopiereUttakTjeneste {

    private RelevanteArbeidsforholdTjeneste relevanteArbeidsforholdTjeneste;

    @Inject
    public SkalKopiereUttakTjeneste(RelevanteArbeidsforholdTjeneste relevanteArbeidsforholdTjeneste) {
        this.relevanteArbeidsforholdTjeneste = relevanteArbeidsforholdTjeneste;
    }

    SkalKopiereUttakTjeneste() {
        //CDI
    }

    /**
     * Skal uttaksstegene kjøres, eller skal resultatet bare kopieres. Eksempel de ikke skal kjøres er revurderinger ved regulering av grunnbeløp.
     * I disse behandlingene skal uttaket ikke endre seg.
     */
    public boolean skalKopiereStegResultat(UttakInput uttakInput) {
        var erRevurdering = uttakInput.getBehandlingReferanse().erRevurdering();
        if (!erRevurdering) {
            return false;
        }
        var arbeidEndret = relevanteArbeidsforholdTjeneste.arbeidsforholdRelevantForUttakErEndretSidenForrigeBehandling(
            uttakInput);
        if (arbeidEndret) {
            return false;
        }
        if (uttakInput.isOpplysningerOmDødEndret() || uttakInput.harBehandlingÅrsakRelatertTilDød()) {
            return false;
        }
        var årsaker = uttakInput.getBehandlingÅrsaker();
        return årsaker.stream()
            .allMatch(å -> å.equals(BehandlingÅrsakType.RE_SATS_REGULERING) || å.equals(
                BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING));
    }

}
