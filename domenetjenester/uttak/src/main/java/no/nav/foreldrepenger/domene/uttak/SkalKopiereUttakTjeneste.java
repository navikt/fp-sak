package no.nav.foreldrepenger.domene.uttak;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class SkalKopiereUttakTjeneste {

    private static final Set<BehandlingÅrsakType> KAN_HOPPE_OVER_UTTAK = Set.of(BehandlingÅrsakType.RE_SATS_REGULERING, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

    private RelevanteArbeidsforholdTjeneste relevanteArbeidsforholdTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public SkalKopiereUttakTjeneste(RelevanteArbeidsforholdTjeneste relevanteArbeidsforholdTjeneste,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.relevanteArbeidsforholdTjeneste = relevanteArbeidsforholdTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
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
        var årsaker = uttakInput.getBehandlingÅrsaker();
        if (årsaker.stream().anyMatch(årsak -> !KAN_HOPPE_OVER_UTTAK.contains(årsak))) {
            return false;
        }
        var arbeidEndret = relevanteArbeidsforholdTjeneste.arbeidsforholdRelevantForUttakErEndretSidenForrigeBehandling(
            uttakInput);
        if (arbeidEndret) {
            return false;
        }
        var fpGrunnlag = (ForeldrepengerGrunnlag) uttakInput.getYtelsespesifiktGrunnlag();
        if (fpGrunnlag.isDødsfall()) {
            return false;
        }
        if (saksbehandlerHarManueltAvklartStartdato(uttakInput)) {
            return false;
        }
        if (familiehendelseEndret(fpGrunnlag)) {
            return false;
        }
        return !årsaker.isEmpty() && KAN_HOPPE_OVER_UTTAK.containsAll(årsaker);
    }

    private boolean familiehendelseEndret(ForeldrepengerGrunnlag fpGrunnlag) {
        var originalBehandling = fpGrunnlag.getOriginalBehandling();
        if (originalBehandling.isEmpty()) {
            return false;
        }
        var familieHendelseDato = fpGrunnlag.getFamilieHendelser()
            .getGjeldendeFamilieHendelse()
            .getFamilieHendelseDato();
        var origFamiliehendelseDato = originalBehandling.get().getFamilieHendelser()
            .getGjeldendeFamilieHendelse()
            .getFamilieHendelseDato();
        return !familieHendelseDato.isEqual(origFamiliehendelseDato);
    }

    private boolean saksbehandlerHarManueltAvklartStartdato(UttakInput uttakInput) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(
            uttakInput.getBehandlingReferanse().behandlingId());
        var avklarteDatoer = ytelseFordelingAggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer);
        return avklarteDatoer.map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato).orElse(null) != null;
    }

}
