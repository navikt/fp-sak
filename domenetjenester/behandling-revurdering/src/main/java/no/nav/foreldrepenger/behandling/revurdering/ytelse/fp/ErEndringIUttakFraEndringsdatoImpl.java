package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.time.LocalDate;
import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIUttakFraEndringsdato;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class ErEndringIUttakFraEndringsdatoImpl implements ErEndringIUttakFraEndringsdato {

    @Override
    public boolean vurder(LocalDate endringsdato, UttakResultatHolder uttakresultatRevurdering, UttakResultatHolder uttakresultatOriginal) {
        return uttakresultatOriginal.vurderOmErEndringIUttakFraEndringsdato(endringsdato,uttakresultatRevurdering);
    }
}
